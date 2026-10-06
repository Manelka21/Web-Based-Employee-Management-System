package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.NotificationDao;
import com.lankatech.ems.dao.impl.rowmapper.NotificationRowMapper;
import com.lankatech.ems.enums.NotificationChannel;
import com.lankatech.ems.model.Notification;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class NotificationDaoImpl extends AbstractJdbcDao<Notification, Integer> implements NotificationDao {

    private final NotificationRowMapper mapper = new NotificationRowMapper();

    public NotificationDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Notification save(Notification n) {
        NotificationChannel channel = n.getChannel() != null ? n.getChannel() : NotificationChannel.WEBSITE;
        // In-app messages are "delivered" the moment they're stored; email/SMS wait in the outbox
        String delivery = n.getDeliveryStatus() != null ? n.getDeliveryStatus()
                : (channel == NotificationChannel.WEBSITE ? "SENT" : "PENDING");

        String sql = "INSERT INTO notifications (user_id, type, message, channel, read_status, delivery_status) VALUES (?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql, n.getUserId(), n.getType(), n.getMessage(), channel.name(), n.isReadStatus(), delivery);
        n.setNotificationId(id);
        n.setChannel(channel);
        n.setDeliveryStatus(delivery);
        return n;
    }

    @Override
    public List<Notification> findByUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND channel = 'WEBSITE' ORDER BY sent_at DESC, notification_id DESC";
        return queryList(sql, mapper, userId);
    }

    @Override
    public List<Notification> findUnreadByUser(int userId) {
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND channel = 'WEBSITE' AND read_status = FALSE " +
                     "ORDER BY sent_at DESC, notification_id DESC";
        return queryList(sql, mapper, userId);
    }

    @Override
    public int markRead(int notificationId, int userId) {
        // user_id in the WHERE clause stops users marking someone else's notifications.
        String sql = "UPDATE notifications SET read_status = TRUE WHERE notification_id = ? AND user_id = ?";
        return executeUpdate(sql, notificationId, userId);
    }

    @Override
    public void markAllRead(int userId) {
        executeUpdate("UPDATE notifications SET read_status = TRUE WHERE user_id = ? AND channel = 'WEBSITE'", userId);
    }

    @Override
    public int countUnread(int userId) {
        return queryForInt("SELECT COUNT(*) FROM notifications WHERE user_id = ? AND channel = 'WEBSITE' AND read_status = FALSE", userId);
    }

    @Override
    public List<Notification> findPendingDeliveries(int limit) {
        String sql = "SELECT * FROM notifications WHERE channel <> 'WEBSITE' AND delivery_status = 'PENDING' " +
                     "ORDER BY notification_id LIMIT ?";
        return queryList(sql, mapper, limit);
    }

    @Override
    public List<Notification> findOutbox(int limit) {
        return queryList("SELECT * FROM notifications WHERE channel <> 'WEBSITE' ORDER BY notification_id DESC LIMIT ?", mapper, limit);
    }

    @Override
    public Optional<Notification> findById(int notificationId) {
        return queryOne("SELECT * FROM notifications WHERE notification_id = ?", mapper, notificationId);
    }

    // Put a failed email back in the queue for the NotificationDispatcher
    @Override
    public void requeue(int notificationId) {
        executeUpdate("UPDATE notifications SET delivery_status = 'PENDING', delivered_at = NULL WHERE notification_id = ?", notificationId);
    }

    @Override
    public void markDelivery(int notificationId, String deliveryStatus) {
        String sql = "UPDATE notifications SET delivery_status = ?, delivered_at = ? WHERE notification_id = ?";
        executeUpdate(sql, deliveryStatus, LocalDateTime.now(), notificationId);
    }
}
