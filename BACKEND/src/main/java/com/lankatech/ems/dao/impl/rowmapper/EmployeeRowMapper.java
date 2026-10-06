package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Gender;
import com.lankatech.ems.model.Employee;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class EmployeeRowMapper implements RowMapper<Employee> {

    @Override
    public Employee map(ResultSet rs) throws SQLException {
        Employee e = new Employee();
        e.setEmployeeId(rs.getInt("employee_id"));
        e.setFirstName(rs.getString("first_name"));
        e.setLastName(rs.getString("last_name"));
        e.setNic(rs.getString("nic"));
        e.setEmail(rs.getString("email"));
        e.setPhone(rs.getString("phone"));
        e.setAddress(rs.getString("address"));

        int deptId = rs.getInt("department_id");
        if (!rs.wasNull()) e.setDepartmentId(deptId);

        int posId = rs.getInt("position_id");
        if (!rs.wasNull()) e.setPositionId(posId);

        Date hire = rs.getDate("hire_date");
        if (hire != null) e.setHireDate(hire.toLocalDate());

        e.setStatus(EmployeeStatus.valueOf(rs.getString("status")));

        String gender = rs.getString("gender");
        if (gender != null) e.setGender(Gender.valueOf(gender));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) e.setCreatedAt(created.toLocalDateTime());

        return e;
    }
}
