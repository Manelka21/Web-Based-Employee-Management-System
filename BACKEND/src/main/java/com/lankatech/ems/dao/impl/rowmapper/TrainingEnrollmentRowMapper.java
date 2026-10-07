package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.EnrollmentStatus;
import com.lankatech.ems.model.TrainingEnrollment;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TrainingEnrollmentRowMapper implements RowMapper<TrainingEnrollment> {

    @Override
    public TrainingEnrollment map(ResultSet rs) throws SQLException {
        TrainingEnrollment en = new TrainingEnrollment();
        en.setEnrollmentId(rs.getInt("enrollment_id"));
        en.setEmployeeId(rs.getInt("employee_id"));
        en.setProgramId(rs.getInt("program_id"));

        Date enrolled = rs.getDate("enrolled_date");
        if (enrolled != null) en.setEnrolledDate(enrolled.toLocalDate());

        en.setCompletionStatus(EnrollmentStatus.valueOf(rs.getString("completion_status")));

        Date completed = rs.getDate("completion_date");
        if (completed != null) en.setCompletionDate(completed.toLocalDate());

        return en;
    }
}
