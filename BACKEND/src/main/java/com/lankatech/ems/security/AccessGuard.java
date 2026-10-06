package com.lankatech.ems.security;

import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.exception.UnauthorizedActionException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.User;
import org.springframework.stereotype.Component;

/**
 * Data-level access rules ("which records can this user touch?").
 *
 * @PreAuthorize on controllers answers "can this ROLE call this endpoint?".
 * AccessGuard answers the finer question, e.g. "is this employee in the
 * supervisor's own department?" or "is this the employee's own record?".
 *
 * The user is always re-read from the database, so a role change or a new
 * employee link takes effect immediately without logging in again.
 */
@Component
public class AccessGuard {

    private final UserDao userDao;
    private final EmployeeDao employeeDao;

    public AccessGuard(UserDao userDao, EmployeeDao employeeDao) {
        this.userDao = userDao;
        this.employeeDao = employeeDao;
    }

    public User currentUser(int userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    // The employee record linked to the logged-in user (needed for self-service features).
    public Employee requireLinkedEmployee(int userId) {
        User user = currentUser(userId);
        if (user.getEmployeeId() == null) {
            throw new UnauthorizedActionException(
                    "Your user account is not linked to an employee record. Ask the IT Administrator to link it.");
        }
        Employee employee = employeeDao.findById(user.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Linked employee not found: " + user.getEmployeeId()));
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new UnauthorizedActionException("Your employee record is inactive. Contact HR.");
        }
        return employee;
    }

    /**
     * Separation of duties: managers may not log, correct, approve or review
     * their own records. Throws if employeeId is the actor's own employee record.
     */
    public void checkNotSelf(int userId, int employeeId, String action) {
        User user = currentUser(userId);
        if (user.getEmployeeId() != null && user.getEmployeeId() == employeeId) {
            throw new UnauthorizedActionException("You cannot " + action + " for yourself");
        }
    }

    public int requireEmployeeId(int userId) {
        return requireLinkedEmployee(userId).getEmployeeId();
    }

    // A supervisor's department is the department of their linked employee record.
    public int requireSupervisorDepartment(int userId) {
        Employee supervisorEmployee = requireLinkedEmployee(userId);
        if (supervisorEmployee.getDepartmentId() == null) {
            throw new UnauthorizedActionException("Your employee record is not assigned to a department");
        }
        return supervisorEmployee.getDepartmentId();
    }

    /**
     * Can this user see/act on data belonging to this employee?
     *   EMPLOYEE          -> only their own record
     *   DEPT_SUPERVISOR   -> employees in their own department
     *   IT_ADMIN          -> never (no access to business data)
     *   HR / PAYROLL / DIRECTOR -> yes (endpoint roles are already limited by @PreAuthorize)
     */
    public void checkEmployeeScope(int userId, int employeeId) {
        User user = currentUser(userId);
        Role role = user.getRole();

        if (role == Role.IT_ADMIN) {
            throw new UnauthorizedActionException("IT Administrators do not have access to employee business data");
        }

        if (role == Role.EMPLOYEE) {
            if (user.getEmployeeId() == null || user.getEmployeeId() != employeeId) {
                throw new UnauthorizedActionException("Employees can only access their own records");
            }
            return;
        }

        if (role == Role.DEPT_SUPERVISOR) {
            if (user.getEmployeeId() != null && user.getEmployeeId() == employeeId) {
                return;   // a supervisor can always see their own record
            }
            int supervisorDept = requireSupervisorDepartment(userId);
            Employee target = employeeDao.findById(employeeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
            if (target.getDepartmentId() == null || target.getDepartmentId() != supervisorDept) {
                throw new UnauthorizedActionException("Supervisors can only access employees in their own department");
            }
        }
    }

    /**
     * Can this user see department-wide data?
     *   DEPT_SUPERVISOR -> only their own department
     *   EMPLOYEE / IT_ADMIN -> no
     *   others -> yes
     */
    public void checkDepartmentScope(int userId, int departmentId) {
        User user = currentUser(userId);
        Role role = user.getRole();

        if (role == Role.EMPLOYEE || role == Role.IT_ADMIN) {
            throw new UnauthorizedActionException("You are not allowed to view department-wide data");
        }
        if (role == Role.DEPT_SUPERVISOR && requireSupervisorDepartment(userId) != departmentId) {
            throw new UnauthorizedActionException("Supervisors can only view their own department");
        }
    }
}
