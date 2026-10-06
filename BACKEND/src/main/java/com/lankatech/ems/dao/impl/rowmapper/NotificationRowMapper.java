package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.NotificationChannel;
import com.lankatech.ems.model.Notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NotificationRowMapper implements RowMapper<Notification> {

    @Override
    public Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getInt("notification_id"));
        n.setUserId(rs.getInt("user_id"));
        n.setType(rs.getString("type"));
        n.setMessage(rs.getString("message"));

        Timestamp sent = rs.getTimestamp("sent_at");
        if (sent != null) n.setSentAt(sent.toLocalDateTime());

        String channel = rs.getString("channel");
        if (channel != null) n.setChannel(NotificationChannel.valueOf(channel));

        n.setReadStatus(rs.getBoolean("read_status"));
        n.setDeliveryStatus(rs.getString("delivery_status"));

        Timestamp delivered = rs.getTimestamp("delivered_at");
        if (delivered != null) n.setDeliveredAt(delivered.toLocalDateTime());

        return n;
    }
}
