package com.lankatech.ems.dao;

import com.lankatech.ems.exception.DataAccessException;
import com.lankatech.ems.exception.DuplicateRecordException;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.*;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Base class for every JDBC DAO.
 *
 * Uses only plain JDBC: DataSource -> Connection -> PreparedStatement -> ResultSet.
 * No JdbcTemplate, no JPA. Every subclass gets these helpers:
 *
 *   queryList(sql, mapper, params)  -> runs a SELECT that returns many rows
 *   queryOne(sql, mapper, params)   -> runs a SELECT that returns 0 or 1 row
 *   queryForInt(sql, params)        -> runs a SELECT COUNT(*) style query
 *   queryForMaps(sql, params)       -> runs a SELECT and returns column->value maps (for reports)
 *   executeUpdate(sql, params)      -> runs INSERT/UPDATE/DELETE, returns rows affected
 *   executeInsertReturnId(sql, ...) -> runs INSERT and returns generated key
 *
 * All of them use try-with-resources so Connection/Statement/ResultSet are
 * always closed, even if an exception is thrown.
 */
public abstract class AbstractJdbcDao<T, ID> {

    // MySQL error codes we translate into meaningful business exceptions
    private static final int MYSQL_DUPLICATE_KEY = 1062;
    private static final int MYSQL_ROW_IS_REFERENCED = 1451;
    private static final int MYSQL_NO_REFERENCED_ROW = 1452;
    private static final int MYSQL_COLUMN_NOT_NULL = 1048;
    private static final int MYSQL_DATA_TOO_LONG = 1406;
    private static final int MYSQL_OUT_OF_RANGE = 1264;
    private static final int MYSQL_CHECK_VIOLATED = 3819;

    protected final DataSource dataSource;

    protected AbstractJdbcDao(DataSource dataSource) {
        // The proxy makes dataSource.getConnection() return the SAME connection
        // inside a @Transactional service method, so several DAO calls can
        // commit or roll back together. Outside a transaction it behaves normally.
        this.dataSource = new TransactionAwareDataSourceProxy(dataSource);
    }

    // ---------- SELECT many rows ----------
    protected List<T> queryList(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapper.map(rs));
                }
            }

        } catch (SQLException e) {
            throw translate("SELECT", sql, e);
        }
        return results;
    }

    // ---------- SELECT one row (or none) ----------
    protected Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> results = queryList(sql, mapper, params);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    // ---------- SELECT a single number (COUNT, SUM ...) ----------
    protected int queryForInt(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }

        } catch (SQLException e) {
            throw translate("SELECT", sql, e);
        }
    }

    // ---------- SELECT rows as column-name -> value maps (used by reports) ----------
    protected List<Map<String, Object>> queryForMaps(String sql, Object... params) {
        List<Map<String, Object>> rows = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParams(ps, params);

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(meta.getColumnLabel(i), fromJdbcValue(rs.getObject(i)));
                    }
                    rows.add(row);
                }
            }

        } catch (SQLException e) {
            throw translate("SELECT", sql, e);
        }
        return rows;
    }

    // ---------- INSERT / UPDATE / DELETE ----------
    protected int executeUpdate(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            bindParams(ps, params);
            return ps.executeUpdate();

        } catch (SQLException e) {
            throw translate("UPDATE", sql, e);
        }
    }

    // ---------- INSERT that returns the auto-generated primary key ----------
    protected int executeInsertReturnId(String sql, Object... params) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindParams(ps, params);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
                throw new DataAccessException("INSERT returned no generated key: " + sql);
            }

        } catch (SQLException e) {
            throw translate("INSERT", sql, e);
        }
    }

    // ---------- helper: bind ?-parameters by index ----------
    private void bindParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, toJdbcValue(params[i]));
        }
    }

    // Java types -> JDBC types. Enums are stored by name (e.g. "PENDING").
    private Object toJdbcValue(Object value) {
        if (value instanceof LocalDate) {
            return Date.valueOf((LocalDate) value);
        }
        if (value instanceof LocalTime) {
            return Time.valueOf((LocalTime) value);
        }
        if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
        }
        if (value instanceof Enum) {
            return ((Enum<?>) value).name();
        }
        return value;
    }

    // JDBC types -> Java types, so JSON output shows readable dates.
    private Object fromJdbcValue(Object value) {
        if (value instanceof Date) {
            return ((Date) value).toLocalDate();
        }
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        }
        return value;
    }

    // ---------- helper: turn a SQLException into one of our exceptions ----------
    private RuntimeException translate(String operation, String sql, SQLException e) {
        if (e.getErrorCode() == MYSQL_DUPLICATE_KEY) {
            return new DuplicateRecordException("A record with the same unique value already exists");
        }
        if (e.getErrorCode() == MYSQL_ROW_IS_REFERENCED) {
            return new IllegalStateException("This record is still used by other records and cannot be removed");
        }
        if (e.getErrorCode() == MYSQL_NO_REFERENCED_ROW) {
            return new IllegalArgumentException("A referenced record (department, position, employee, ...) does not exist");
        }
        // Bad input that slipped past validation must be a 400, not a 500
        if (e.getErrorCode() == MYSQL_COLUMN_NOT_NULL) {
            return new IllegalArgumentException("A required value is missing: " + e.getMessage());
        }
        if (e.getErrorCode() == MYSQL_DATA_TOO_LONG) {
            return new IllegalArgumentException("A value is too long for its field: " + e.getMessage());
        }
        if (e.getErrorCode() == MYSQL_OUT_OF_RANGE) {
            return new IllegalArgumentException("A number is out of the allowed range: " + e.getMessage());
        }
        if (e.getErrorCode() == MYSQL_CHECK_VIOLATED) {
            return new IllegalArgumentException("The data breaks a database rule: " + e.getMessage());
        }
        return new DataAccessException(operation + " failed: " + sql, e);
    }
}
