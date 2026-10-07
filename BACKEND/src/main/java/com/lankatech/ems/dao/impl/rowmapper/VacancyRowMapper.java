package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.VacancyStatus;
import com.lankatech.ems.model.Vacancy;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class VacancyRowMapper implements RowMapper<Vacancy> {

    @Override
    public Vacancy map(ResultSet rs) throws SQLException {
        Vacancy v = new Vacancy();
        v.setVacancyId(rs.getInt("vacancy_id"));
        v.setTitle(rs.getString("title"));
        v.setDepartmentId(rs.getInt("department_id"));
        v.setRequirements(rs.getString("requirements"));

        Date deadline = rs.getDate("deadline");
        if (deadline != null) v.setDeadline(deadline.toLocalDate());

        v.setStatus(VacancyStatus.valueOf(rs.getString("status")));
        v.setCreatedBy(rs.getInt("created_by"));

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) v.setCreatedAt(created.toLocalDateTime());

        return v;
    }
}
