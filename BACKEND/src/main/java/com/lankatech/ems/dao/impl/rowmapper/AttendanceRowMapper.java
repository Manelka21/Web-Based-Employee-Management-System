package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.AttendanceStatus;
import com.lankatech.ems.model.AttendanceRecord;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

public class AttendanceRowMapper implements RowMapper<AttendanceRecord> {

    @Override
    public AttendanceRecord map(ResultSet rs) throws SQLException {
        AttendanceRecord a = new AttendanceRecord();

        // inherited EmployeeEvent fields
        a.setEventId(rs.getInt("attendance_id"));
        a.setEmployeeId(rs.getInt("employee_id"));

        Date date = rs.getDate("date");
        if (date != null) a.setEventDate(date.toLocalDate());

        // AttendanceRecord fields
        Time checkIn = rs.getTime("check_in_time");
        if (checkIn != null) a.setCheckInTime(checkIn.toLocalTime());

        Time checkOut = rs.getTime("check_out_time");
        if (checkOut != null) a.setCheckOutTime(checkOut.toLocalTime());

        a.setAttendanceStatus(AttendanceStatus.valueOf(rs.getString("status")));   // also sets inherited status
        a.setOverrideReason(rs.getString("override_reason"));                      // also sets inherited notes

        return a;
    }
}
