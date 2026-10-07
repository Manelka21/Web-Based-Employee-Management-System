package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.NotificationDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.enums.NotificationChannel;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Notification;
import com.lankatech.ems.model.User;
import com.lankatech.ems.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cross-cutting: called by Leave, Attendance, Payroll, Recruitment, Training
 * and Performance services. Notifications are stored in-app only; the
 * channel column is there so EMAIL/SMS sending can be added later.
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_MESSAGE_LENGTH = 500;

    private final NotificationDao notificationDao;
    private final UserDao userDao;

    public NotificationServiceImpl(NotificationDao notificationDao, UserDao userDao) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
    }

    // Notifications important enough to also be emailed (via the outbox + NotificationDispatcher)
    private static final Set<String> EMAIL_TYPES = Set.of(
            "LEAVE_APPROVED", "LEAVE_REJECTED", "LEAVE_EXPIRED", "PAYSLIP_AVAILABLE", "SALARY_PAID",
            "ACCOUNT_LOCKED", "PASSWORD_RESET", "ROLE_CHANGED");

    @Override
    public void send(int userId, String type, String message) {
        send(userId, type, message, NotificationChannel.WEBSITE);
        if (EMAIL_TYPES.contains(type)) {
            send(userId, type, message, NotificationChannel.EMAIL);
        }
    }

    // Overloaded: specify channel explicitly (polymorphism — overloading)
    @Override
    public void send(int userId, String type, String message, NotificationChannel channel) {
        Notification n = new Notification(userId, type, truncate(message), channel);
        notificationDao.save(n);
    }

    @Override
    public void sendToEmployee(int employeeId, String type, String message) {
        Optional<User> account = userDao.findByEmployeeId(employeeId);
        // Not every employee has a login yet (e.g. a newly hired candidate)
        account.ifPresent(user -> send(user.getUserId(), type, message));
    }

    @Override
    public void sendToRole(Role role, String type, String message) {
        for (User user : userDao.findByRole(role.name())) {
            send(user.getUserId(), type, message);
        }
    }

    @Override
    public List<Notification> findMine(int userId) {
        return notificationDao.findByUser(userId);
    }

    @Override
    public List<Notification> findUnread(int userId) {
        return notificationDao.findUnreadByUser(userId);
    }

    @Override
    public int countUnread(int userId) {
        return notificationDao.countUnread(userId);
    }

    @Override
    public void markRead(int notificationId, int userId) {
        int updated = notificationDao.markRead(notificationId, userId);
        if (updated == 0) {
            throw new ResourceNotFoundException("Notification not found: " + notificationId);
        }
    }

    @Override
    public void markAllRead(int userId) {
        notificationDao.markAllRead(userId);
    }

    @Override
    public List<Notification> outbox(int limit) {
        return notificationDao.findOutbox(Math.max(1, Math.min(limit, 500)));
    }

    // Only FAILED emails can be retried; the dispatcher picks them up within a minute
    @Override
    public Notification retry(int notificationId, int adminUserId) {
        Notification n = notificationDao.findById(notificationId)
                .filter(x -> x.getChannel() != NotificationChannel.WEBSITE)
                .orElseThrow(() -> new ResourceNotFoundException("Outbox message not found: " + notificationId));
        if (!"FAILED".equals(n.getDeliveryStatus())) {
            throw new IllegalArgumentException("Only FAILED messages can be retried (this one is " + n.getDeliveryStatus() + ")");
        }
        notificationDao.requeue(notificationId);
        return notificationDao.findById(notificationId).orElseThrow();
    }

    private String truncate(String text) {
        if (text == null || text.length() <= MAX_MESSAGE_LENGTH) {
            return text;
        }
        return text.substring(0, MAX_MESSAGE_LENGTH);
    }
}
