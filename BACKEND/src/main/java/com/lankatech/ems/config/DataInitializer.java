package com.lankatech.ems.config;

import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.exception.DataAccessException;
import com.lankatech.ems.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Runs once when the application starts.
 *
 * schema.sql inserts the seed users with the placeholder password
 * 'CHANGE_ME_ON_STARTUP'. This class replaces each placeholder with a real
 * BCrypt hash of "password123", so nobody has to generate hashes by hand.
 * Users whose password is already a real hash are not touched.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String PLACEHOLDER = "CHANGE_ME_ON_STARTUP";
    private static final String DEFAULT_PASSWORD = "password123";

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        try {
            int updated = 0;
            for (User user : userDao.findAll()) {
                if (PLACEHOLDER.equals(user.getPasswordHash())) {
                    userDao.updatePassword(user.getUserId(), passwordEncoder.encode(DEFAULT_PASSWORD));
                    updated++;
                }
            }
            if (updated > 0) {
                log.info("Set the default password '{}' for {} seed user(s)", DEFAULT_PASSWORD, updated);
            }
        } catch (DataAccessException e) {
            log.error("Could not read the users table. Did you run src/main/resources/schema.sql in MySQL "
                    + "and set the correct username/password in application.properties? Cause: {}",
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }
}
