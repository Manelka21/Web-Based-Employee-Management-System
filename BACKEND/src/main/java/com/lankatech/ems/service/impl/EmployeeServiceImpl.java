package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.JobPositionDao;
import com.lankatech.ems.dao.LeaveRequestDao;
import com.lankatech.ems.dto.request.RegisterEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateProfileRequest;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Gender;
import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.JobPosition;
import com.lankatech.ems.model.LeaveRequest;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.EmployeeService;
import com.lankatech.ems.util.EnumUtils;
import com.lankatech.ems.util.ValidationRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private static final LocalDate EARLIEST_HIRE_DATE = LocalDate.of(1970, 1, 1);

    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final JobPositionDao jobPositionDao;
    private final LeaveRequestDao leaveRequestDao;
    private final AccessGuard accessGuard;
    private final ActivityLogService activityLogService;

    public EmployeeServiceImpl(EmployeeDao employeeDao, DepartmentDao departmentDao, JobPositionDao jobPositionDao,
                               LeaveRequestDao leaveRequestDao, AccessGuard accessGuard,
                               ActivityLogService activityLogService) {
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.jobPositionDao = jobPositionDao;
        this.leaveRequestDao = leaveRequestDao;
        this.accessGuard = accessGuard;
        this.activityLogService = activityLogService;
    }

    @Override
    public Employee register(RegisterEmployeeRequest r, int actorUserId) {
        Employee e = new Employee();
        e.setFirstName(r.getFirstName().trim());
        e.setLastName(r.getLastName().trim());
        e.setNic(ValidationRules.normalizeNic(r.getNic()));
        e.setEmail(ValidationRules.normalizeEmail(r.getEmail()));
        e.setPhone(ValidationRules.normalizePhone(r.getPhone()));
        e.setAddress(r.getAddress());
        e.setDepartmentId(r.getDepartmentId());
        e.setPositionId(r.getPositionId());
        e.setHireDate(r.getHireDate());
        if (r.getStatus() != null) {
            e.setStatus(EnumUtils.parse(EmployeeStatus.class, r.getStatus(), "status"));
        }
        if (r.getGender() != null && !r.getGender().isBlank()) {
            e.setGender(EnumUtils.parse(Gender.class, r.getGender(), "gender"));
        }
        return create(e, actorUserId);
    }

    // Also used by the recruitment "hire" flow.
    @Override
    public Employee create(Employee employee, int actorUserId) {
        if (employee.getNic() == null || employee.getNic().isBlank()) {
            throw new IllegalArgumentException("NIC is required to create an employee record");
        }
        if (employeeDao.existsByNic(employee.getNic())) {
            throw new DuplicateRecordException("Employee with NIC " + employee.getNic() + " already exists");
        }
        if (employeeDao.existsByEmail(employee.getEmail())) {
            throw new DuplicateRecordException("Employee with email " + employee.getEmail() + " already exists");
        }
        validateAssignment(employee.getDepartmentId(), employee.getPositionId());

        if (employee.getStatus() == null) {
            employee.setStatus(EmployeeStatus.ACTIVE);
        }
        if (employee.getStatus() == EmployeeStatus.INACTIVE || employee.getStatus() == EmployeeStatus.ON_LEAVE) {
            throw new IllegalArgumentException("A new employee must start as ACTIVE or PROBATION");
        }
        if (employee.getHireDate() == null) {
            employee.setHireDate(LocalDate.now());
        }
        // Business rule: hire date within a sensible window (back-dated records allowed,
        // future hires at most 6 months ahead)
        if (employee.getHireDate().isBefore(EARLIEST_HIRE_DATE)) {
            throw new IllegalArgumentException("Hire date cannot be before " + EARLIEST_HIRE_DATE);
        }
        if (employee.getHireDate().isAfter(LocalDate.now().plusMonths(6))) {
            throw new IllegalArgumentException("Hire date cannot be more than 6 months in the future");
        }

        Employee saved = employeeDao.save(employee);
        activityLogService.log(actorUserId, "CREATE_EMPLOYEE",
                "Created employee #" + saved.getEmployeeId() + " " + saved.getFullName());
        return saved;
    }

    @Override
    public Employee getById(int employeeId) {
        return employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    @Override
    public Employee getForUser(int employeeId, int userId) {
        Employee employee = getById(employeeId);
        accessGuard.checkEmployeeScope(userId, employeeId);
        return employee;
    }

    @Override
    public Employee getMyProfile(int userId) {
        return accessGuard.requireLinkedEmployee(userId);
    }

    @Override
    public List<Employee> findAll() {
        return employeeDao.findAll();
    }

    @Override
    public List<Employee> findByDepartment(int departmentId, int userId) {
        accessGuard.checkDepartmentScope(userId, departmentId);
        return employeeDao.findByDepartment(departmentId);
    }

    // Partial update: only fields present in the request are changed.
    @Override
    public Employee update(int employeeId, UpdateEmployeeRequest r, int actorUserId) {
        Employee existing = getById(employeeId);

        if (r.getFirstName() != null && !r.getFirstName().isBlank()) {
            existing.setFirstName(r.getFirstName().trim());
        }
        if (r.getLastName() != null && !r.getLastName().isBlank()) {
            existing.setLastName(r.getLastName().trim());
        }
        // A blank email is ignored: every employee record must keep a work email
        if (r.getEmail() != null && !r.getEmail().isBlank() && !r.getEmail().trim().equalsIgnoreCase(existing.getEmail())) {
            String newEmail = r.getEmail().trim().toLowerCase();
            if (employeeDao.existsByEmail(newEmail)) {
                throw new DuplicateRecordException("Employee with email " + newEmail + " already exists");
            }
            existing.setEmail(newEmail);
        }
        if (r.getPhone() != null) {
            existing.setPhone(ValidationRules.normalizePhone(r.getPhone()));
        }
        if (r.getAddress() != null) {
            existing.setAddress(r.getAddress().trim());
        }
        if (r.getGender() != null && !r.getGender().isBlank()) {
            existing.setGender(EnumUtils.parse(Gender.class, r.getGender(), "gender"));
        }
        // Explicit clearing (a null id alone means "unchanged")
        if (Boolean.TRUE.equals(r.getClearDepartment())) {
            if (r.getDepartmentId() != null) {
                throw new IllegalArgumentException("Send either a departmentId or clearDepartment, not both");
            }
            existing.setDepartmentId(null);
            existing.setPositionId(null);   // a position always belongs to a department
        } else if (Boolean.TRUE.equals(r.getClearPosition())) {
            if (r.getPositionId() != null) {
                throw new IllegalArgumentException("Send either a positionId or clearPosition, not both");
            }
            existing.setPositionId(null);
        }
        if (r.getDepartmentId() != null || r.getPositionId() != null) {
            boolean departmentChanged = r.getDepartmentId() != null && !r.getDepartmentId().equals(existing.getDepartmentId());
            if (r.getDepartmentId() != null) {
                existing.setDepartmentId(r.getDepartmentId());
            }
            if (r.getPositionId() != null) {
                existing.setPositionId(r.getPositionId());
            } else if (departmentChanged && existing.getPositionId() != null) {
                // Moved to another department without a new position: the old position
                // belongs to the previous department, so unassign it instead of failing.
                boolean positionFitsNewDepartment = jobPositionDao.findById(existing.getPositionId())
                        .map(p -> p.getDepartmentId() == existing.getDepartmentId())
                        .orElse(false);
                if (!positionFitsNewDepartment) {
                    existing.setPositionId(null);
                }
            }
            validateAssignment(existing.getDepartmentId(), existing.getPositionId());
        }
        boolean deactivating = false;
        if (r.getStatus() != null) {
            EmployeeStatus newStatus = EnumUtils.parse(EmployeeStatus.class, r.getStatus(), "status");
            deactivating = newStatus == EmployeeStatus.INACTIVE && existing.getStatus() != EmployeeStatus.INACTIVE;
            existing.setStatus(newStatus);
        }

        employeeDao.update(existing);
        if (deactivating) {
            // Same clean-up as DELETE /api/employees/{id}
            cleanUpAfterDeactivation(existing, actorUserId);
        }
        activityLogService.log(actorUserId, "UPDATE_EMPLOYEE", "Updated employee #" + employeeId);
        return getById(employeeId);
    }

    // Self-service: an employee can change only their phone and address.
    @Override
    public Employee updateMyProfile(int userId, UpdateProfileRequest r) {
        Employee me = accessGuard.requireLinkedEmployee(userId);
        if (r.getPhone() != null) {
            me.setPhone(ValidationRules.normalizePhone(r.getPhone()));
        }
        if (r.getAddress() != null) {
            me.setAddress(r.getAddress().trim());
        }
        employeeDao.update(me);
        activityLogService.log(userId, "UPDATE_OWN_PROFILE", "Employee #" + me.getEmployeeId() + " updated contact details");
        return getById(me.getEmployeeId());
    }

    /**
     * Permanent delete. The employee and their attendance, leave, training, reviews,
     * payroll and salary are removed in one transaction; their login is kept (the
     * activity log refers to it) but unlinked and disabled. To keep the history
     * instead, set the employee's status to INACTIVE in the edit form.
     */
    @Override
    @Transactional
    public void delete(int employeeId, int actorUserId) {
        Employee existing = getById(employeeId);
        accessGuard.checkNotSelf(actorUserId, employeeId, "delete the employee record");
        employeeDao.deleteWithHistory(employeeId);
        activityLogService.log(actorUserId, "DELETE_EMPLOYEE",
                "Deleted employee #" + employeeId + " " + existing.getFullName() + " (NIC " + existing.getNic() + ") and their records");
    }

    /**
     * Business rules when an employee leaves:
     *   - they stop being head of any department
     *   - their PENDING leave requests are cancelled (otherwise they block payroll forever)
     *   - their linked user account can no longer sign in (see AppUserDetailsService)
     */
    private void cleanUpAfterDeactivation(Employee employee, int actorUserId) {
        for (Department department : departmentDao.findAll()) {
            if (department.getHeadOfDeptId() != null && department.getHeadOfDeptId() == employee.getEmployeeId()) {
                department.setHeadOfDeptId(null);
                departmentDao.update(department);
                activityLogService.log(actorUserId, "UNSET_DEPARTMENT_HEAD",
                        "Employee #" + employee.getEmployeeId() + " removed as head of " + department.getName());
            }
        }
        for (LeaveRequest leave : leaveRequestDao.findByEmployee(employee.getEmployeeId())) {
            if (leave.getLeaveStatus() == LeaveStatus.PENDING) {
                leave.setLeaveStatus(LeaveStatus.CANCELLED);
                leaveRequestDao.updateStatus(leave);
            }
        }
    }

    // Business rule: the department must be active and the position must belong to that department.
    private void validateAssignment(Integer departmentId, Integer positionId) {
        if (departmentId != null) {
            Department department = departmentDao.findById(departmentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
            if (!"ACTIVE".equals(department.getStatus())) {
                throw new IllegalArgumentException("Department '" + department.getName() + "' is inactive");
            }
        }
        if (positionId != null) {
            JobPosition position = jobPositionDao.findById(positionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job position not found: " + positionId));
            if (departmentId == null) {
                throw new IllegalArgumentException("Choose a department before assigning the position '" + position.getTitle() + "'");
            }
            if (position.getDepartmentId() != departmentId) {
                throw new IllegalArgumentException("Position '" + position.getTitle()
                        + "' does not belong to department #" + departmentId);
            }
        }
    }
}
