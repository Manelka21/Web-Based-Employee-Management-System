package com.lankatech.ems.dao;

import com.lankatech.ems.model.LeaveRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestDao {

    LeaveRequest save(LeaveRequest leave);
    Optional<LeaveRequest> findById(int leaveId);
    List<LeaveRequest> findByEmployee(int employeeId);
    List<LeaveRequest> findByStatus(String status);
    List<LeaveRequest> findPendingByDepartment(int departmentId);
    List<LeaveRequest> findPendingForEmployeeInPeriod(int employeeId, LocalDate start, LocalDate end);
    List<LeaveRequest> findActiveOverlapping(int employeeId, LocalDate start, LocalDate end);
    // PENDING/APPROVED requests of one type that touch the given calendar year (handles New Year spans)
    List<LeaveRequest> findActiveByEmployeeTypeOverlappingYear(int employeeId, String leaveType, int year);
    List<LeaveRequest> findOverlappingYear(int year);
    List<LeaveRequest> findApprovedCovering(LocalDate date);
    List<LeaveRequest> findPendingStartedBefore(LocalDate date);
    void updateStatus(LeaveRequest leave);
    int countByStatus(String status);
    int countPendingByDepartment(int departmentId);
}
