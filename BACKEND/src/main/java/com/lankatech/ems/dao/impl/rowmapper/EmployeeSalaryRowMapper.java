package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.EmployeeSalary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class EmployeeSalaryRowMapper implements RowMapper<EmployeeSalary> {

    @Override
    public EmployeeSalary map(ResultSet rs) throws SQLException {
        EmployeeSalary s = new EmployeeSalary();
        s.setEmployeeId(rs.getInt("employee_id"));
        s.setBaseSalary(rs.getBigDecimal("base_salary"));

        int updatedBy = rs.getInt("updated_by");
        if (!rs.wasNull()) s.setUpdatedBy(updatedBy);

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) s.setUpdatedAt(updated.toLocalDateTime());

        return s;
    }
}
