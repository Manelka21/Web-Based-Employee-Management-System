package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.AttendanceDao;
import com.lankatech.ems.dao.impl.rowmapper.AttendanceRowMapper;
import com.lankatech.ems.model.AttendanceRecord;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class AttendanceDaoImpl extends AbstractJdbcDao<AttendanceRecord, Integer> implements AttendanceDao {

    private final AttendanceRowMapper mapper = new AttendanceRowMapper();

    public AttendanceDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public AttendanceRecord save(AttendanceRecord a) {
        String sql =
            "INSERT INTO attendance_records (employee_id, date, check_in_time, check_out_time, status, override_reason) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            a.getEmployeeId(), a.getEventDate(), a.getCheckInTime(), a.getCheckOutTime(),
            a.getAttendanceStatus().name(), a.getOverrideReason()
        );
        a.setEventId(id);
        return a;
    }

    @Override
    public Optional<AttendanceRecord> findById(int attendanceId) {
        return queryOne("SELECT * FROM attendance_records WHERE attendance_id = ?", mapper, attendanceId);
    }

    @Override
    public Optional<AttendanceRecord> findByEmployeeAndDate(int employeeId, LocalDate date) {
        return queryOne("SELECT * FROM attendance_records WHERE employee_id = ? AND date = ?", mapper, employeeId, date);
    }

    @Override
    public List<AttendanceRecord> findByEmployee(int employeeId) {
        return queryList("SELECT * FROM attendance_records WHERE employee_id = ? ORDER BY date DESC", mapper, employeeId);
    }

    @Override
    public List<AttendanceRecord> findByDepartment(int departmentId) {
        String sql =
            "SELECT a.* FROM attendance_records a " +
            "JOIN employees e ON a.employee_id = e.employee_id " +
            "WHERE e.department_id = ? ORDER BY a.date DESC, a.employee_id";
        return queryList(sql, mapper, departmentId);
    }

    @Override
    public List<AttendanceRecord> findByEmployeeAndPeriod(int employeeId, LocalDate start, LocalDate end) {
        String sql = "SELECT * FROM attendance_records WHERE employee_id = ? AND date BETWEEN ? AND ? ORDER BY date";
        return queryList(sql, mapper, employeeId, start, end);
    }

    @Override
    public void update(AttendanceRecord a) {
        String sql =
            "UPDATE attendance_records SET check_in_time = ?, check_out_time = ?, status = ?, override_reason = ? " +
            "WHERE attendance_id = ?";
        executeUpdate(sql,
            a.getCheckInTime(), a.getCheckOutTime(), a.getAttendanceStatus().name(), a.getOverrideReason(), a.getEventId()
        );
    }

    @Override
    public void deleteById(int attendanceId) {
        executeUpdate("DELETE FROM attendance_records WHERE attendance_id = ?", attendanceId);
    }

    @Override
    public int countByDate(LocalDate date) {
        return queryForInt("SELECT COUNT(*) FROM attendance_records WHERE date = ? AND status <> 'ABSENT'", date);
    }
}
