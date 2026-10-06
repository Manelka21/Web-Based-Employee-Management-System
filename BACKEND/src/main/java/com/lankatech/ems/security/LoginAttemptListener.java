package com.lankatech.ems.security;

import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.model.User;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.NotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Brute-force protection. Spring Security publishes an event for every sign-in
 * attempt (JSON login and HTTP Basic alike):
 *   - wrong password  -> count it; after MAX_ATTEMPTS the account is locked for LOCK_MINUTES
 *   - right password  -> reset the counter
 * While locked, AppUserPrincipal.isAccountNonLocked() is false, so Spring refuses
 * the sign-in before even checking the password.
 */
@Component
public class LoginAttemptListener {

    public static final int MAX_ATTEMPTS = 5;
    public static final int LOCK_MINUTES = 15;

    private final UserDao userDao;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public LoginAttemptListener(UserDao userDao, NotificationService notificationService,
                                ActivityLogService activityLogService) {
        this.userDao = userDao;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        Optional<User> found = userDao.findByEmail(String.valueOf(event.getAuthentication().getPrincipal()).trim());
        if (found.isEmpty()) {
            return;   // unknown email: nothing to lock
        }
        User user = found.get();
        int attempts = user.getFailedLoginAttempts() + 1;

        if (attempts >= MAX_ATTEMPTS) {
            LocalDateTime until = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
            userDao.recordFailedLogin(user.getUserId(), 0, until);
            activityLogService.log(user.getUserId(), "ACCOUNT_LOCKED",
                    "Locked for " + LOCK_MINUTES + " minutes after " + MAX_ATTEMPTS + " wrong passwords");
            notificationService.send(user.getUserId(), "ACCOUNT_LOCKED",
                    "Your account was locked for " + LOCK_MINUTES + " minutes after " + MAX_ATTEMPTS
                            + " wrong password attempts. If this wasn't you, contact your IT administrator.");
        } else {
            userDao.recordFailedLogin(user.getUserId(), attempts, null);
        }
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication().getPrincipal() instanceof AppUserPrincipal principal) {
            User user = principal.getUser();
            // Only write when there is something to clear (Basic auth succeeds on every request)
            if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
                userDao.clearLoginFailures(user.getUserId());
            }
        }
    }
}
