package com.lankatech.ems.model;

import com.lankatech.ems.enums.NotificationChannel;
import java.time.LocalDateTime;

public class Notification {

    private int notificationId;
    private int userId;
    private String type;
    private String message;
    private LocalDateTime sentAt;
    private NotificationChannel channel;
    private boolean readStatus;
    private String deliveryStatus;          // SENT for in-app; PENDING / SENT / FAILED for EMAIL
    private LocalDateTime deliveredAt;

    public Notification() {
    }

    public Notification(int userId, String type, String message, NotificationChannel channel) {
        this.userId = userId;
        this.type = type;
        this.message = message;
        this.channel = channel;
        this.readStatus = false;
    }

    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public boolean isReadStatus() { return readStatus; }
    public void setReadStatus(boolean readStatus) { this.readStatus = readStatus; }

    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}
