package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.dao.impl.rowmapper.UserRowMapper;
import com.lankatech.ems.model.User;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class UserDaoImpl extends AbstractJdbcDao<User, Integer> implements UserDao {

    private final UserRowMapper mapper = new UserRowMapper();

    public UserDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public User save(User user) {
        String sql =
            "INSERT INTO users (email, password_hash, first_name, last_name, phone_number, role, employee_id) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?)";

        int generatedId = executeInsertReturnId(sql,
            user.getEmail(),
            user.getPasswordHash(),
            user.getFirstName(),
            user.getLastName(),
            user.getPhoneNumber(),
            user.getRole().name(),
            user.getEmployeeId()
        );
        user.setUserId(generatedId);
        return user;
    }

    @Override
    public Optional<User> findById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        return queryOne(sql, mapper, userId);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return queryOne(sql, mapper, email);
    }

    @Override
    public Optional<User> findByEmployeeId(int employeeId) {
        String sql = "SELECT * FROM users WHERE employee_id = ?";
        return queryOne(sql, mapper, employeeId);
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY user_id";
        return queryList(sql, mapper);
    }

    @Override
    public List<User> findByRole(String role) {
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY user_id";
        return queryList(sql, mapper, role);
    }

    @Override
    public List<User> findSupervisorsByDepartment(int departmentId) {
        // A supervisor "belongs" to a department through their linked employee record.
        String sql =
            "SELECT u.* FROM users u " +
            "JOIN employees e ON u.employee_id = e.employee_id " +
            "WHERE u.role = 'DEPT_SUPERVISOR' AND e.department_id = ?";
        return queryList(sql, mapper, departmentId);
    }

    @Override
    public void linkEmployee(int userId, int employeeId) {
        String sql = "UPDATE users SET employee_id = ? WHERE user_id = ?";
        executeUpdate(sql, employeeId, userId);
    }

    @Override
    public void updateRole(int userId, String role) {
        String sql = "UPDATE users SET role = ? WHERE user_id = ?";
        executeUpdate(sql, role, userId);
    }

    @Override
    public void updatePassword(int userId, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        executeUpdate(sql, passwordHash, userId);
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public void setActive(int userId, boolean active) {
        executeUpdate("UPDATE users SET active = ? WHERE user_id = ?", active, userId);
    }

    @Override
    public void setMustChangePassword(int userId, boolean mustChange) {
        executeUpdate("UPDATE users SET must_change_password = ? WHERE user_id = ?", mustChange, userId);
    }

    @Override
    public void recordFailedLogin(int userId, int attempts, LocalDateTime lockedUntil) {
        String sql = "UPDATE users SET failed_login_attempts = ?, locked_until = ? WHERE user_id = ?";
        executeUpdate(sql, attempts, lockedUntil, userId);
    }

    @Override
    public void clearLoginFailures(int userId) {
        executeUpdate("UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE user_id = ?", userId);
    }
}
