package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.ApplicationStatus;
import com.lankatech.ems.model.CandidateApplication;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class CandidateApplicationRowMapper implements RowMapper<CandidateApplication> {

    @Override
    public CandidateApplication map(ResultSet rs) throws SQLException {
        CandidateApplication a = new CandidateApplication();
        a.setApplicationId(rs.getInt("application_id"));
        a.setVacancyId(rs.getInt("vacancy_id"));
        a.setCandidateName(rs.getString("candidate_name"));
        a.setCandidateEmail(rs.getString("candidate_email"));
        a.setCandidatePhone(rs.getString("candidate_phone"));
        a.setCandidateNic(rs.getString("candidate_nic"));
        a.setResumeNotes(rs.getString("resume_notes"));
        a.setStatus(ApplicationStatus.valueOf(rs.getString("status")));

        Date applied = rs.getDate("applied_date");
        if (applied != null) a.setAppliedDate(applied.toLocalDate());

        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) a.setUpdatedAt(updated.toLocalDateTime());

        return a;
    }
}
