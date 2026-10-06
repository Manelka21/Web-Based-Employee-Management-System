package com.lankatech.ems.dao;

import com.lankatech.ems.model.Notification;
import java.util.List;

public interface NotificationDao {

    Notification save(Notification notification);

    // In-app inbox (WEBSITE channel only)
    List<Notification> findByUser(int userId);
    List<Notification> findUnreadByUser(int userId);
    int markRead(int notificationId, int userId);
    void markAllRead(int userId);
    int countUnread(int userId);

    // Email/SMS outbox
    List<Notification> findPendingDeliveries(int limit);
    void markDelivery(int notificationId, String deliveryStatus);
    List<Notification> findOutbox(int limit);
    java.util.Optional<Notification> findById(int notificationId);
    void requeue(int notificationId);
}
