package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.LeaveRequestDao;
import com.lankatech.ems.dao.impl.rowmapper.LeaveRequestRowMapper;
import com.lankatech.ems.model.LeaveRequest;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class LeaveRequestDaoImpl extends AbstractJdbcDao<LeaveRequest, Integer> implements LeaveRequestDao {

    private final LeaveRequestRowMapper mapper = new LeaveRequestRowMapper();

    public LeaveRequestDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public LeaveRequest save(LeaveRequest l) {
        String sql =
            "INSERT INTO leave_requests (employee_id, leave_type, start_date, end_date, reason, status) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            l.getEmployeeId(), l.getLeaveType().name(), l.getStartDate(), l.getEndDate(),
            l.getReason(), l.getLeaveStatus().name()
        );
        l.setEventId(id);
        return l;
    }

    @Override
    public Optional<LeaveRequest> findById(int leaveId) {
        return queryOne("SELECT * FROM leave_requests WHERE leave_id = ?", mapper, leaveId);
    }

    @Override
    public List<LeaveRequest> findByEmployee(int employeeId) {
        return queryList("SELECT * FROM leave_requests WHERE employee_id = ? ORDER BY start_date DESC", mapper, employeeId);
    }

    @Override
    public List<LeaveRequest> findByStatus(String status) {
        return queryList("SELECT * FROM leave_requests WHERE status = ? ORDER BY submitted_date", mapper, status);
    }

    @Override
    public List<LeaveRequest> findPendingByDepartment(int departmentId) {
        String sql =
            "SELECT l.* FROM leave_requests l " +
            "JOIN employees e ON l.employee_id = e.employee_id " +
            "WHERE l.status = 'PENDING' AND e.department_id = ? ORDER BY l.submitted_date";
        return queryList(sql, mapper, departmentId);
    }

    @Override
    public List<LeaveRequest> findPendingForEmployeeInPeriod(int employeeId, LocalDate start, LocalDate end) {
        // Two date ranges overlap when: leave.start <= period.end AND leave.end >= period.start
        String sql =
            "SELECT * FROM leave_requests " +
            "WHERE employee_id = ? AND status = ? AND start_date <= ? AND end_date >= ?";
        return queryList(sql, mapper, employeeId, "PENDING", end, start);
    }

    @Override
    public List<LeaveRequest> findActiveOverlapping(int employeeId, LocalDate start, LocalDate end) {
        String sql =
            "SELECT * FROM leave_requests " +
            "WHERE employee_id = ? AND status IN ('PENDING', 'APPROVED') AND start_date <= ? AND end_date >= ?";
        return queryList(sql, mapper, employeeId, end, start);
    }

    @Override
    public List<LeaveRequest> findActiveByEmployeeTypeOverlappingYear(int employeeId, String leaveType, int year) {
        String sql =
            "SELECT * FROM leave_requests " +
            "WHERE employee_id = ? AND leave_type = ? AND status IN ('PENDING', 'APPROVED') " +
            "AND start_date <= ? AND end_date >= ?";
        return queryList(sql, mapper, employeeId, leaveType, LocalDate.of(year, 12, 31), LocalDate.of(year, 1, 1));
    }

    @Override
    public List<LeaveRequest> findOverlappingYear(int year) {
        String sql = "SELECT * FROM leave_requests WHERE start_date <= ? AND end_date >= ? ORDER BY start_date";
        return queryList(sql, mapper, LocalDate.of(year, 12, 31), LocalDate.of(year, 1, 1));
    }

    @Override
    public List<LeaveRequest> findApprovedCovering(LocalDate date) {
        String sql = "SELECT * FROM leave_requests WHERE status = 'APPROVED' AND start_date <= ? AND end_date >= ?";
        return queryList(sql, mapper, date, date);
    }

    @Override
    public List<LeaveRequest> findPendingStartedBefore(LocalDate date) {
        String sql = "SELECT * FROM leave_requests WHERE status = 'PENDING' AND start_date < ? ORDER BY start_date";
        return queryList(sql, mapper, date);
    }

    @Override
    public void updateStatus(LeaveRequest l) {
        String sql = "UPDATE leave_requests SET status = ?, approved_by = ?, approved_date = ? WHERE leave_id = ?";
        executeUpdate(sql, l.getLeaveStatus().name(), l.getApprovedBy(), l.getApprovedDate(), l.getEventId());
    }

    @Override
    public int countByStatus(String status) {
        return queryForInt("SELECT COUNT(*) FROM leave_requests WHERE status = ?", status);
    }

    @Override
    public int countPendingByDepartment(int departmentId) {
        String sql =
            "SELECT COUNT(*) FROM leave_requests l " +
            "JOIN employees e ON l.employee_id = e.employee_id " +
            "WHERE l.status = 'PENDING' AND e.department_id = ?";
        return queryForInt(sql, departmentId);
    }
}
