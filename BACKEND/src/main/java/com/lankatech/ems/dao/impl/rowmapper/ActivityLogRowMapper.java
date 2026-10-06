package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.model.ActivityLog;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ActivityLogRowMapper implements RowMapper<ActivityLog> {

    @Override
    public ActivityLog map(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setLogId(rs.getInt("log_id"));
        log.setUserId(rs.getInt("user_id"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));

        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) log.setTimestamp(ts.toLocalDateTime());

        return log;
    }
}
