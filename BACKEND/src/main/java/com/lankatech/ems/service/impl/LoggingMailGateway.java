package com.lankatech.ems.service.impl;

import com.lankatech.ems.service.MailGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Development stand-in for an SMTP sender: the "email" appears in the application log.
@Component
public class LoggingMailGateway implements MailGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMailGateway.class);

    @Override
    public boolean send(String to, String subject, String body) {
        log.info("[EMAIL] to={} subject=\"{}\" body=\"{}\"", to, subject, body);
        return true;
    }
}
