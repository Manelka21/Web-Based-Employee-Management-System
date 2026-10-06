package com.lankatech.ems.dao;

import com.lankatech.ems.model.User;
import java.util.List;
import java.util.Optional;

public interface UserDao {

    User save(User user);
    Optional<User> findById(int userId);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmployeeId(int employeeId);
    List<User> findAll();
    List<User> findByRole(String role);
    List<User> findSupervisorsByDepartment(int departmentId);
    void linkEmployee(int userId, int employeeId);
    void updateRole(int userId, String role);
    void updatePassword(int userId, String passwordHash);
    boolean existsByEmail(String email);
    void setActive(int userId, boolean active);
    void setMustChangePassword(int userId, boolean mustChange);
    void recordFailedLogin(int userId, int attempts, java.time.LocalDateTime lockedUntil);
    void clearLoginFailures(int userId);
}
