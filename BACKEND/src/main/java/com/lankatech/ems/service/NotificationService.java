package com.lankatech.ems.service;

import com.lankatech.ems.enums.NotificationChannel;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.model.Notification;

import java.util.List;

public interface NotificationService {

    // Overloading: same name, different parameter lists
    void send(int userId, String type, String message);
    void send(int userId, String type, String message, NotificationChannel channel);

    // Notify the user account linked to an employee (does nothing if there is none)
    void sendToEmployee(int employeeId, String type, String message);

    // Notify every user with the given role
    void sendToRole(Role role, String type, String message);

    List<Notification> findMine(int userId);
    List<Notification> findUnread(int userId);
    int countUnread(int userId);
    void markRead(int notificationId, int userId);
    void markAllRead(int userId);

    // ---------- email outbox (IT Admin) ----------
    List<Notification> outbox(int limit);
    Notification retry(int notificationId, int adminUserId);
}
