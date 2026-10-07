package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.DepartmentDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.VacancyDao;
import com.lankatech.ems.dto.request.CreateDepartmentRequest;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentServiceImpl implements DepartmentService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final DepartmentDao departmentDao;
    private final EmployeeDao employeeDao;
    private final VacancyDao vacancyDao;
    private final ActivityLogService activityLogService;

    public DepartmentServiceImpl(DepartmentDao departmentDao, EmployeeDao employeeDao, VacancyDao vacancyDao,
                                 ActivityLogService activityLogService) {
        this.departmentDao = departmentDao;
        this.employeeDao = employeeDao;
        this.vacancyDao = vacancyDao;
        this.activityLogService = activityLogService;
    }

    @Override
    public Department create(CreateDepartmentRequest r, int actorUserId) {
        String name = r.getName().trim();
        if (departmentDao.existsByName(name)) {
            throw new DuplicateRecordException("Department '" + name + "' already exists");
        }
        validateHead(r.getHeadOfDeptId(), null);

        Department department = new Department(name, r.getDescription());
        department.setHeadOfDeptId(r.getHeadOfDeptId());
        department.setStatus(parseStatus(r.getStatus(), ACTIVE));

        Department saved = departmentDao.save(department);
        activityLogService.log(actorUserId, "CREATE_DEPARTMENT",
                "Created department #" + saved.getDepartmentId() + " " + saved.getName());
        return saved;
    }

    @Override
    public List<Department> findAll() {
        return departmentDao.findAll();
    }

    @Override
    public Department findById(int departmentId) {
        return departmentDao.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));
    }

    @Override
    public Department update(int departmentId, CreateDepartmentRequest r, int actorUserId) {
        Department existing = findById(departmentId);

        String name = r.getName().trim();
        Optional<Department> sameName = departmentDao.findByName(name);
        if (sameName.isPresent() && sameName.get().getDepartmentId() != departmentId) {
            throw new DuplicateRecordException("Department '" + name + "' already exists");
        }
        validateHead(r.getHeadOfDeptId(), departmentId);

        // Deactivating through the edit form must follow the same rules as DELETE
        String newStatus = parseStatus(r.getStatus(), existing.getStatus());
        if (INACTIVE.equals(newStatus) && !INACTIVE.equals(existing.getStatus())) {
            checkCanDeactivate(existing);
        }

        existing.setName(name);
        existing.setDescription(r.getDescription());
        existing.setHeadOfDeptId(r.getHeadOfDeptId());
        existing.setStatus(parseStatus(r.getStatus(), existing.getStatus()));

        departmentDao.update(existing);
        activityLogService.log(actorUserId, "UPDATE_DEPARTMENT", "Updated department #" + departmentId);
        return findById(departmentId);
    }

    /**
     * Permanent delete. Only an empty department can go: employees must be moved, and
     * vacancies / training programs (which keep recruitment and training history)
     * deleted or moved first. Its job positions are deleted with it.
     */
    @Override
    @Transactional
    public void delete(int departmentId, int actorUserId) {
        Department existing = findById(departmentId);
        String name = "'" + existing.getName() + "'";

        int employees = employeeDao.countByDepartment(departmentId);
        if (employees > 0) {
            throw new IllegalStateException("Cannot delete " + name + ": " + employees
                    + " employee(s) still belong to it. Move them to another department first.");
        }
        int vacancies = departmentDao.countVacancies(departmentId);
        if (vacancies > 0) {
            throw new IllegalStateException("Cannot delete " + name + ": it has " + vacancies
                    + " vacancy(ies) in Recruitment. Delete them first, or set the department to Inactive instead.");
        }
        int programs = departmentDao.countTrainingPrograms(departmentId);
        if (programs > 0) {
            throw new IllegalStateException("Cannot delete " + name + ": it has " + programs
                    + " training program(s). Delete them or make them company-wide first, or set the department to Inactive instead.");
        }

        departmentDao.deleteWithPositions(departmentId);
        activityLogService.log(actorUserId, "DELETE_DEPARTMENT", "Deleted department #" + departmentId + " " + existing.getName());
    }

    // Business rule: the head must be an active employee, and (for an existing
    // department) must work in that department.
    private void validateHead(Integer headOfDeptId, Integer departmentId) {
        if (headOfDeptId == null) {
            return;
        }
        Employee head = employeeDao.findById(headOfDeptId)
                .orElseThrow(() -> new ResourceNotFoundException("Head of department (employee #" + headOfDeptId + ") not found"));
        if (head.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException(head.getFullName() + " is inactive and cannot head a department");
        }
        if (departmentId != null && !departmentId.equals(head.getDepartmentId())) {
            throw new IllegalArgumentException(head.getFullName() + " does not work in this department");
        }
    }

    // Business rules: employees must be moved and open vacancies closed first.
    private void checkCanDeactivate(Department department) {
        int employeeCount = employeeDao.countByDepartment(department.getDepartmentId());
        if (employeeCount > 0) {
            throw new IllegalStateException("Cannot deactivate '" + department.getName() + "': "
                    + employeeCount + " employee(s) are still assigned to it");
        }
        int openVacancies = vacancyDao.countOpenByDepartment(department.getDepartmentId());
        if (openVacancies > 0) {
            throw new IllegalStateException("Cannot deactivate '" + department.getName() + "': "
                    + openVacancies + " vacancy(ies) are still open. Close them first.");
        }
    }

    private String parseStatus(String status, String defaultStatus) {
        if (status == null || status.isBlank()) {
            return defaultStatus;
        }
        String upper = status.trim().toUpperCase();
        if (!ACTIVE.equals(upper) && !INACTIVE.equals(upper)) {
            throw new IllegalArgumentException("Department status must be ACTIVE or INACTIVE");
        }
        return upper;
    }
}
