package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.impl.rowmapper.EmployeeRowMapper;
import com.lankatech.ems.model.Employee;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeDaoImpl extends AbstractJdbcDao<Employee, Integer> implements EmployeeDao {

    private final EmployeeRowMapper mapper = new EmployeeRowMapper();

    public EmployeeDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Employee save(Employee e) {
        String sql =
            "INSERT INTO employees (first_name, last_name, nic, email, phone, address, " +
            "department_id, position_id, hire_date, status, gender) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int id = executeInsertReturnId(sql,
            e.getFirstName(), e.getLastName(), e.getNic(), e.getEmail(),
            e.getPhone(), e.getAddress(), e.getDepartmentId(), e.getPositionId(),
            e.getHireDate(),
            e.getStatus() != null ? e.getStatus().name() : "ACTIVE",
            e.getGender()
        );
        e.setEmployeeId(id);
        return e;
    }

    @Override
    public Optional<Employee> findById(int employeeId) {
        return queryOne("SELECT * FROM employees WHERE employee_id = ?", mapper, employeeId);
    }

    @Override
    public Optional<Employee> findByNic(String nic) {
        return queryOne("SELECT * FROM employees WHERE nic = ?", mapper, nic);
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return queryOne("SELECT * FROM employees WHERE email = ?", mapper, email);
    }

    @Override
    public List<Employee> findAll() {
        return queryList("SELECT * FROM employees ORDER BY employee_id", mapper);
    }

    @Override
    public List<Employee> findByDepartment(int departmentId) {
        return queryList("SELECT * FROM employees WHERE department_id = ? ORDER BY last_name", mapper, departmentId);
    }

    @Override
    public void update(Employee e) {
        String sql =
            "UPDATE employees SET first_name = ?, last_name = ?, email = ?, phone = ?, " +
            "address = ?, department_id = ?, position_id = ?, status = ?, gender = ? WHERE employee_id = ?";
        executeUpdate(sql,
            e.getFirstName(), e.getLastName(), e.getEmail(), e.getPhone(),
            e.getAddress(), e.getDepartmentId(), e.getPositionId(),
            e.getStatus().name(), e.getGender(), e.getEmployeeId()
        );
    }

    @Override
    public void updateStatus(int employeeId, String status) {
        executeUpdate("UPDATE employees SET status = ? WHERE employee_id = ?", status, employeeId);
    }

    @Override
    public boolean existsByNic(String nic) {
        return findByNic(nic).isPresent();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public int countAll() {
        return queryForInt("SELECT COUNT(*) FROM employees");
    }

    @Override
    public int countByStatus(String status) {
        return queryForInt("SELECT COUNT(*) FROM employees WHERE status = ?", status);
    }

    @Override
    public int countByDepartment(int departmentId) {
        return queryForInt("SELECT COUNT(*) FROM employees WHERE department_id = ?", departmentId);
    }

    @Override
    public int countByPosition(int positionId) {
        return queryForInt("SELECT COUNT(*) FROM employees WHERE position_id = ?", positionId);
    }

    /**
     * Child rows first, then the employee (foreign keys would refuse it otherwise):
     *   attendance, leave, training enrollments, performance reviews, payroll, salary
     *   are deleted; the login account is kept for the audit trail but unlinked and
     *   disabled; any department this employee headed loses its head.
     */
    @Override
    public void deleteWithHistory(int employeeId) {
        executeUpdate("DELETE FROM attendance_records WHERE employee_id = ?", employeeId);
        executeUpdate("DELETE FROM leave_requests WHERE employee_id = ?", employeeId);
        executeUpdate("DELETE FROM training_enrollments WHERE employee_id = ?", employeeId);
        executeUpdate("DELETE FROM performance_records WHERE employee_id = ?", employeeId);
        executeUpdate("DELETE FROM payroll_records WHERE employee_id = ?", employeeId);
        executeUpdate("DELETE FROM employee_salaries WHERE employee_id = ?", employeeId);
        executeUpdate("UPDATE users SET employee_id = NULL, active = FALSE WHERE employee_id = ?", employeeId);
        executeUpdate("UPDATE departments SET head_of_dept_id = NULL WHERE head_of_dept_id = ?", employeeId);
        executeUpdate("DELETE FROM employees WHERE employee_id = ?", employeeId);
    }
}
