package com.lankatech.ems.security;

import com.lankatech.ems.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Wraps our User model in Spring Security's UserDetails interface.
public class AppUserPrincipal implements UserDetails {

    private final User user;
    private final boolean enabled;

    public AppUserPrincipal(User user) {
        this(user, true);
    }

    // enabled = false when the linked employee record has been deactivated
    public AppUserPrincipal(User user, boolean enabled) {
        this.user = user;
        this.enabled = enabled;
    }

    public User getUser() {
        return user;
    }

    public int getUserId() {
        return user.getUserId();
    }

    public Integer getEmployeeId() {
        return user.getEmployeeId();
    }

    // Spring Security uses "ROLE_" prefix for role-based rules.
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();      // we log in with email
    }

    @Override public boolean isAccountNonExpired()     { return true; }
    // Locked for 15 minutes after 5 wrong passwords (see LoginAttemptListener)
    @Override public boolean isAccountNonLocked()      { return !user.isLocked(); }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return enabled; }
}
