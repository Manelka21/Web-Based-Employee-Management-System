package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.Department;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DepartmentRowMapper implements RowMapper<Department> {

    @Override
    public Department map(ResultSet rs) throws SQLException {
        Department d = new Department();
        d.setDepartmentId(rs.getInt("department_id"));
        d.setName(rs.getString("name"));
        d.setDescription(rs.getString("description"));

        int head = rs.getInt("head_of_dept_id");
        if (!rs.wasNull()) d.setHeadOfDeptId(head);

        d.setStatus(rs.getString("status"));
        return d;
    }
}
