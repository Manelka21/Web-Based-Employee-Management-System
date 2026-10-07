package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.PerformanceDao;
import com.lankatech.ems.dto.request.RecordPerformanceRequest;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.exception.UnauthorizedActionException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.PerformanceRecord;
import com.lankatech.ems.model.User;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.service.PerformanceService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceServiceImpl implements PerformanceService {

    private final PerformanceDao performanceDao;
    private final EmployeeDao employeeDao;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public PerformanceServiceImpl(PerformanceDao performanceDao, EmployeeDao employeeDao, AccessGuard accessGuard,
                                  NotificationService notificationService, ActivityLogService activityLogService) {
        this.performanceDao = performanceDao;
        this.employeeDao = employeeDao;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Override
    public PerformanceRecord create(RecordPerformanceRequest r, int userId) {
        if (r.getEmployeeId() == null) {
            throw new IllegalArgumentException("employeeId is required");
        }

        Employee employee = employeeDao.findById(r.getEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + r.getEmployeeId()));

        User supervisor = accessGuard.currentUser(userId);
        if (supervisor.getEmployeeId() != null && supervisor.getEmployeeId() == employee.getEmployeeId()) {
            throw new UnauthorizedActionException("You cannot record performance feedback for yourself");
        }

        // Business rule: supervisor can only record feedback for employees in their own department.
        // The supervisor's department comes from their linked employee record (see AccessGuard).
        accessGuard.checkEmployeeScope(userId, employee.getEmployeeId());

        LocalDate reviewDate = r.getReviewDate() != null ? r.getReviewDate() : LocalDate.now();
        if (reviewDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Review date cannot be in the future");
        }
        if (employee.getHireDate() != null && reviewDate.isBefore(employee.getHireDate())) {
            throw new IllegalArgumentException("Review date cannot be before the hire date (" + employee.getHireDate() + ")");
        }
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException(employee.getFullName() + " is inactive and can't be reviewed");
        }
        for (PerformanceRecord existing : performanceDao.findByEmployee(employee.getEmployeeId())) {
            if (existing.getSupervisorId() == userId && reviewDate.equals(existing.getReviewDate())) {
                throw new DuplicateRecordException("You already reviewed " + employee.getFullName() + " on " + reviewDate
                        + ". Edit that review instead.");
            }
        }

        PerformanceRecord record = new PerformanceRecord();
        record.setEmployeeId(employee.getEmployeeId());
        record.setSupervisorId(userId);
        record.setFeedback(r.getFeedback().trim());
        record.setRating(r.getRating());
        record.setReviewDate(reviewDate);

        PerformanceRecord saved = performanceDao.save(record);

        notificationService.sendToEmployee(employee.getEmployeeId(), "PERFORMANCE_FEEDBACK",
                "New performance feedback from " + supervisor.getFullName()
                        + (saved.getRating() != null ? " (rating " + saved.getRating() + "/5)" : ""));
        activityLogService.log(userId, "RECORD_PERFORMANCE",
                "Performance record #" + saved.getPerformanceId() + " for employee #" + employee.getEmployeeId());
        return requireRecord(saved.getPerformanceId());
    }

    @Override
    public List<PerformanceRecord> findByEmployee(int employeeId, int userId) {
        accessGuard.checkEmployeeScope(userId, employeeId);
        return performanceDao.findByEmployee(employeeId);
    }

    @Override
    public List<PerformanceRecord> findByTeam(int departmentId, int userId) {
        accessGuard.checkDepartmentScope(userId, departmentId);
        return performanceDao.findByDepartment(departmentId);
    }

    @Override
    public List<PerformanceRecord> findMine(int userId) {
        return performanceDao.findByEmployee(accessGuard.requireEmployeeId(userId));
    }

    // Only the supervisor who wrote the feedback can edit it.
    @Override
    public PerformanceRecord update(int performanceId, RecordPerformanceRequest r, int userId) {
        PerformanceRecord record = requireRecord(performanceId);
        if (record.getSupervisorId() != userId) {
            throw new UnauthorizedActionException("You can only edit feedback that you recorded");
        }
        if (r.getReviewDate() != null && r.getReviewDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Review date cannot be in the future");
        }

        record.setFeedback(r.getFeedback().trim());
        record.setRating(r.getRating());
        if (r.getReviewDate() != null) {
            record.setReviewDate(r.getReviewDate());
        }

        performanceDao.update(record);
        activityLogService.log(userId, "UPDATE_PERFORMANCE", "Edited performance record #" + performanceId);
        return requireRecord(performanceId);
    }

    // The author supervisor or an HR Manager can remove a record.
    @Override
    public void delete(int performanceId, int userId) {
        PerformanceRecord record = requireRecord(performanceId);
        User user = accessGuard.currentUser(userId);
        if (user.getRole() != Role.HR_MANAGER && record.getSupervisorId() != userId) {
            throw new UnauthorizedActionException("You can only remove feedback that you recorded");
        }
        performanceDao.deleteById(performanceId);
        activityLogService.log(userId, "DELETE_PERFORMANCE",
                "Removed performance record #" + performanceId + " for employee #" + record.getEmployeeId());
    }

    private PerformanceRecord requireRecord(int performanceId) {
        return performanceDao.findById(performanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Performance record not found: " + performanceId));
    }
}
