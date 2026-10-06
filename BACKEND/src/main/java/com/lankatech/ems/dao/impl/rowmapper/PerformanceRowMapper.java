package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.PerformanceRecord;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PerformanceRowMapper implements RowMapper<PerformanceRecord> {

    @Override
    public PerformanceRecord map(ResultSet rs) throws SQLException {
        PerformanceRecord p = new PerformanceRecord();
        p.setPerformanceId(rs.getInt("performance_id"));
        p.setEmployeeId(rs.getInt("employee_id"));
        p.setSupervisorId(rs.getInt("supervisor_id"));
        p.setFeedback(rs.getString("feedback"));

        int rating = rs.getInt("rating");
        if (!rs.wasNull()) p.setRating(rating);

        Date review = rs.getDate("review_date");
        if (review != null) p.setReviewDate(review.toLocalDate());

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) p.setCreatedAt(created.toLocalDateTime());

        return p;
    }
}
