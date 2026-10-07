package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.ProgramStatus;
import com.lankatech.ems.model.TrainingProgram;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TrainingProgramRowMapper implements RowMapper<TrainingProgram> {

    @Override
    public TrainingProgram map(ResultSet rs) throws SQLException {
        TrainingProgram p = new TrainingProgram();
        p.setProgramId(rs.getInt("program_id"));
        p.setTitle(rs.getString("title"));
        p.setTrainer(rs.getString("trainer"));

        Date start = rs.getDate("start_date");
        if (start != null) p.setStartDate(start.toLocalDate());

        Date end = rs.getDate("end_date");
        if (end != null) p.setEndDate(end.toLocalDate());

        int deptId = rs.getInt("department_id");
        if (!rs.wasNull()) p.setDepartmentId(deptId);

        int capacity = rs.getInt("capacity");
        if (!rs.wasNull()) p.setCapacity(capacity);

        p.setDescription(rs.getString("description"));
        p.setStatus(ProgramStatus.valueOf(rs.getString("status")));

        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) p.setCreatedBy(createdBy);

        return p;
    }
}
