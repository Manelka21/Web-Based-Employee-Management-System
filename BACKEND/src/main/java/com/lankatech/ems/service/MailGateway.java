package com.lankatech.ems.service;

/**
 * Sends one email. The NotificationDispatcher calls this for every queued EMAIL notification.
 *
 * The default implementation (LoggingMailGateway) only writes the email to the log,
 * because no SMTP server is configured. To send real email, add
 * spring-boot-starter-mail, set spring.mail.* in application.properties and replace
 * the gateway with one that uses JavaMailSender — nothing else has to change.
 */
public interface MailGateway {

    // Returns true when the message was handed over successfully
    boolean send(String to, String subject, String body);
}
