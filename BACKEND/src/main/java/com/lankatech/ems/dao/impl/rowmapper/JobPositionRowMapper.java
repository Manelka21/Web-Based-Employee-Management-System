package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.JobPosition;

import java.sql.ResultSet;
import java.sql.SQLException;

public class JobPositionRowMapper implements RowMapper<JobPosition> {

    @Override
    public JobPosition map(ResultSet rs) throws SQLException {
        JobPosition p = new JobPosition();
        p.setPositionId(rs.getInt("position_id"));
        p.setTitle(rs.getString("title"));
        p.setDepartmentId(rs.getInt("department_id"));
        p.setDescription(rs.getString("description"));
        return p;
    }
}
