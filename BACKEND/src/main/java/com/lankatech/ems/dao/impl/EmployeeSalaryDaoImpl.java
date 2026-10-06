package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.EmployeeSalaryDao;
import com.lankatech.ems.dao.impl.rowmapper.EmployeeSalaryRowMapper;
import com.lankatech.ems.model.EmployeeSalary;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public class EmployeeSalaryDaoImpl extends AbstractJdbcDao<EmployeeSalary, Integer> implements EmployeeSalaryDao {

    private final EmployeeSalaryRowMapper mapper = new EmployeeSalaryRowMapper();

    public EmployeeSalaryDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Optional<EmployeeSalary> findByEmployee(int employeeId) {
        return queryOne("SELECT * FROM employee_salaries WHERE employee_id = ?", mapper, employeeId);
    }

    @Override
    public List<EmployeeSalary> findAll() {
        return queryList("SELECT * FROM employee_salaries ORDER BY employee_id", mapper);
    }

    // Insert, or update if the employee already has a salary on file
    @Override
    public void upsert(int employeeId, BigDecimal baseSalary, int updatedBy) {
        String sql =
            "INSERT INTO employee_salaries (employee_id, base_salary, updated_by) VALUES (?, ?, ?) " +
            "ON DUPLICATE KEY UPDATE base_salary = VALUES(base_salary), updated_by = VALUES(updated_by)";
        executeUpdate(sql, employeeId, baseSalary, updatedBy);
    }
}
