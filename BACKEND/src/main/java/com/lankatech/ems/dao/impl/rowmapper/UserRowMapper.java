package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.model.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Maps a row from the users table to a User object.
 * There are no subclass-specific columns per role, so this is a flat mapping.
 */
public class UserRowMapper implements RowMapper<User> {

    @Override
    public User map(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setPhoneNumber(rs.getString("phone_number"));
        user.setRole(Role.valueOf(rs.getString("role")));

        int empId = rs.getInt("employee_id");
        if (!rs.wasNull()) user.setEmployeeId(empId);

        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) user.setCreatedAt(created.toLocalDateTime());

        user.setActive(rs.getBoolean("active"));
        user.setMustChangePassword(rs.getBoolean("must_change_password"));
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));

        Timestamp lockedUntil = rs.getTimestamp("locked_until");
        if (lockedUntil != null) user.setLockedUntil(lockedUntil.toLocalDateTime());

        return user;
    }
}
