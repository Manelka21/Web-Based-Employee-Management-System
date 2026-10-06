package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.enums.LeaveType;
import com.lankatech.ems.model.LeaveRequest;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class LeaveRequestRowMapper implements RowMapper<LeaveRequest> {

    @Override
    public LeaveRequest map(ResultSet rs) throws SQLException {
        LeaveRequest l = new LeaveRequest();

        // inherited EmployeeEvent fields
        l.setEventId(rs.getInt("leave_id"));
        l.setEmployeeId(rs.getInt("employee_id"));

        // LeaveRequest fields (the setters also keep eventDate/status/notes in sync)
        l.setLeaveType(LeaveType.valueOf(rs.getString("leave_type")));

        Date start = rs.getDate("start_date");
        if (start != null) l.setStartDate(start.toLocalDate());

        Date end = rs.getDate("end_date");
        if (end != null) l.setEndDate(end.toLocalDate());

        l.setReason(rs.getString("reason"));
        l.setLeaveStatus(LeaveStatus.valueOf(rs.getString("status")));

        int approvedBy = rs.getInt("approved_by");
        if (!rs.wasNull()) l.setApprovedBy(approvedBy);

        Timestamp approved = rs.getTimestamp("approved_date");
        if (approved != null) l.setApprovedDate(approved.toLocalDateTime());

        Timestamp submitted = rs.getTimestamp("submitted_date");
        if (submitted != null) l.setSubmittedDate(submitted.toLocalDateTime());

        return l;
    }
}
