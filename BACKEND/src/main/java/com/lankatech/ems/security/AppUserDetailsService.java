package com.lankatech.ems.security;

import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.model.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Loads a user from our UserDao when Spring Security asks for one.
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserDao userDao;
    private final EmployeeDao employeeDao;

    public AppUserDetailsService(UserDao userDao, EmployeeDao employeeDao) {
        this.userDao = userDao;
        this.employeeDao = employeeDao;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userDao.findByEmail(email.trim())
                .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
        return new AppUserPrincipal(user, isActive(user));
    }

    /**
     * An account can sign in only when:
     *   - the IT Administrator hasn't disabled it (users.active), and
     *   - its linked employee record (if any) isn't INACTIVE.
     */
    public boolean isActive(User user) {
        if (!user.isActive()) {
            return false;
        }
        if (user.getEmployeeId() == null) {
            return true;
        }
        return employeeDao.findById(user.getEmployeeId())
                .map(e -> e.getStatus() != EmployeeStatus.INACTIVE)
                .orElse(true);
    }
}
