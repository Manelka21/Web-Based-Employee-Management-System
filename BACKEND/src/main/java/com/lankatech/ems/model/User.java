package com.lankatech.ems.model;

import com.lankatech.ems.enums.Role;
import java.time.LocalDateTime;

// Flat user class: all six roles share the same columns.
// Role-specific behaviour lives in SecurityConfig + @PreAuthorize, not in subclasses.
public class User {

    private int userId;
    private String email;
    private String passwordHash;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private Role role;
    private Integer employeeId;               // nullable: set once a User is linked to an Employee record
    private LocalDateTime createdAt;
    private boolean active = true;               // false = disabled by the IT Administrator
    private boolean mustChangePassword;          // true after an admin reset
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;           // set after too many wrong passwords

    public User() {
    }

    // Locked while lockedUntil is in the future
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public User(String email, String passwordHash, String firstName, String lastName, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // -- getters and setters --
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public boolean isMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }

    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(int failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }
}
