package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.PublicHoliday;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PublicHolidayRowMapper implements RowMapper<PublicHoliday> {

    @Override
    public PublicHoliday map(ResultSet rs) throws SQLException {
        PublicHoliday h = new PublicHoliday();
        h.setHolidayDate(rs.getDate("holiday_date").toLocalDate());
        h.setName(rs.getString("name"));

        int createdBy = rs.getInt("created_by");
        if (!rs.wasNull()) h.setCreatedBy(createdBy);

        return h;
    }
}
