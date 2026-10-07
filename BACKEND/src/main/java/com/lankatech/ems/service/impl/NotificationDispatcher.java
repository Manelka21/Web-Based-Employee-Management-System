package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.NotificationDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.exception.DataAccessException;
import com.lankatech.ems.model.Notification;
import com.lankatech.ems.model.User;
import com.lankatech.ems.service.MailGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Email outbox. NotificationServiceImpl queues EMAIL copies of important notifications
 * with delivery_status = PENDING; this job hands them to the MailGateway once a minute
 * and records SENT or FAILED. Sending outside the request means a slow or broken mail
 * server never slows down or fails the user's action.
 */
@Component
public class NotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final int BATCH_SIZE = 50;

    private final NotificationDao notificationDao;
    private final UserDao userDao;
    private final MailGateway mailGateway;

    public NotificationDispatcher(NotificationDao notificationDao, UserDao userDao, MailGateway mailGateway) {
        this.notificationDao = notificationDao;
        this.userDao = userDao;
        this.mailGateway = mailGateway;
    }

    @Scheduled(fixedDelay = 60_000, initialDelay = 15_000)
    public void deliverPending() {
        try {
            for (Notification n : notificationDao.findPendingDeliveries(BATCH_SIZE)) {
                Optional<User> user = userDao.findById(n.getUserId());
                boolean sent = false;
                try {
                    sent = user.isPresent() && mailGateway.send(user.get().getEmail(), subjectFor(n.getType()), n.getMessage());
                } catch (RuntimeException e) {
                    log.warn("Email #{} failed: {}", n.getNotificationId(), e.getMessage());
                }
                notificationDao.markDelivery(n.getNotificationId(), sent ? "SENT" : "FAILED");
            }
        } catch (DataAccessException e) {
            log.warn("Email dispatch skipped: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    private String subjectFor(String type) {
        String readable = type == null ? "Update" : type.replace('_', ' ').toLowerCase();
        return "LankaTech EMS: " + Character.toUpperCase(readable.charAt(0)) + readable.substring(1);
    }
}
