package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.AttendanceDao;
import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.LeaveRequestDao;
import com.lankatech.ems.dto.request.LogAttendanceRequest;
import com.lankatech.ems.enums.AttendanceStatus;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.InvalidStatusTransitionException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.AttendanceRecord;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.AttendanceService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.util.EnumUtils;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    // Checking in after this time counts as LATE
    private static final LocalTime OFFICE_START = LocalTime.of(9, 0);

    // Fewer minutes than this between check-in and check-out counts as HALF_DAY
    private static final long HALF_DAY_MINUTES = 4 * 60;

    private final AttendanceDao attendanceDao;
    private final EmployeeDao employeeDao;
    private final LeaveRequestDao leaveRequestDao;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public AttendanceServiceImpl(AttendanceDao attendanceDao, EmployeeDao employeeDao, LeaveRequestDao leaveRequestDao,
                                 AccessGuard accessGuard, NotificationService notificationService,
                                 ActivityLogService activityLogService) {
        this.attendanceDao = attendanceDao;
        this.employeeDao = employeeDao;
        this.leaveRequestDao = leaveRequestDao;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    // Supervisor / HR logs attendance for an employee
    @Override
    public AttendanceRecord log(LogAttendanceRequest r, int userId) {
        if (r.getEmployeeId() == null || r.getDate() == null) {
            throw new IllegalArgumentException("employeeId and date are required");
        }
        if (r.getDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Attendance cannot be logged for a future date");
        }

        Employee employee = requireEmployee(r.getEmployeeId());
        accessGuard.checkEmployeeScope(userId, employee.getEmployeeId());
        accessGuard.checkNotSelf(userId, employee.getEmployeeId(), "log attendance (use check-in/check-out)");

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException("Cannot log attendance for an inactive employee");
        }
        if (employee.getHireDate() != null && r.getDate().isBefore(employee.getHireDate())) {
            throw new IllegalArgumentException("Attendance cannot be logged before the hire date (" + employee.getHireDate() + ")");
        }
        AttendanceStatus requested = EnumUtils.parse(AttendanceStatus.class, r.getStatus(), "status");
        if (requested != AttendanceStatus.ABSENT && isOnApprovedLeave(employee.getEmployeeId(), r.getDate())) {
            throw new IllegalArgumentException(employee.getFullName() + " is on approved leave on " + r.getDate());
        }
        if (attendanceDao.findByEmployeeAndDate(employee.getEmployeeId(), r.getDate()).isPresent()) {
            throw new DuplicateRecordException("Attendance for employee #" + employee.getEmployeeId()
                    + " on " + r.getDate() + " already exists. Correct it instead (PUT).");
        }

        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(employee.getEmployeeId());
        record.setEventDate(r.getDate());
        applyFields(record, r);

        AttendanceRecord saved = attendanceDao.save(record);
        // Polymorphism: applyEffect() is AttendanceRecord's version here
        activityLogService.log(userId, "LOG_ATTENDANCE",
                "Attendance #" + saved.getEventId() + " for employee #" + saved.getEmployeeId() + " on "
                        + saved.getEventDate() + " (" + saved.getAttendanceStatus() + "): " + saved.applyEffect());
        return saved;
    }

    // Employee self-service check-in for today
    @Override
    public AttendanceRecord checkIn(int userId) {
        Employee me = accessGuard.requireLinkedEmployee(userId);
        LocalDate today = LocalDate.now();

        if (attendanceDao.findByEmployeeAndDate(me.getEmployeeId(), today).isPresent()) {
            throw new DuplicateRecordException("You have already checked in today");
        }
        if (isOnApprovedLeave(me.getEmployeeId(), today)) {
            throw new IllegalArgumentException("You are on approved leave today. Cancel the leave first if you are working.");
        }

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployeeId(me.getEmployeeId());
        record.setEventDate(today);
        record.setCheckInTime(now);
        record.setAttendanceStatus(now.isAfter(OFFICE_START) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT);

        AttendanceRecord saved = attendanceDao.save(record);
        activityLogService.log(userId, "CHECK_IN", "Employee #" + me.getEmployeeId() + " checked in at " + now);
        return saved;
    }

    // Employee self-service check-out for today
    @Override
    public AttendanceRecord checkOut(int userId) {
        Employee me = accessGuard.requireLinkedEmployee(userId);
        AttendanceRecord record = attendanceDao.findByEmployeeAndDate(me.getEmployeeId(), LocalDate.now())
                .orElseThrow(() -> new InvalidStatusTransitionException("You have not checked in today"));

        if (record.getCheckOutTime() != null) {
            throw new InvalidStatusTransitionException("You have already checked out today");
        }

        LocalTime now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES);
        if (record.getCheckInTime() != null && !now.isAfter(record.getCheckInTime())) {
            throw new InvalidStatusTransitionException("Check-out must be after your check-in time (" + record.getCheckInTime() + ")");
        }
        record.setCheckOutTime(now);
        // Business rule: less than 4 hours worked counts as a half day
        if (record.getCheckInTime() != null
                && Duration.between(record.getCheckInTime(), now).toMinutes() < HALF_DAY_MINUTES) {
            record.setAttendanceStatus(AttendanceStatus.HALF_DAY);
        }
        attendanceDao.update(record);
        activityLogService.log(userId, "CHECK_OUT", "Employee #" + me.getEmployeeId() + " checked out at " + now);
        return requireRecord(record.getEventId());
    }

    @Override
    public List<AttendanceRecord> findByEmployee(int employeeId, int userId) {
        requireEmployee(employeeId);
        accessGuard.checkEmployeeScope(userId, employeeId);
        return attendanceDao.findByEmployee(employeeId);
    }

    @Override
    public List<AttendanceRecord> findMine(int userId) {
        return attendanceDao.findByEmployee(accessGuard.requireEmployeeId(userId));
    }

    @Override
    public List<AttendanceRecord> findByDepartment(int departmentId, int userId) {
        accessGuard.checkDepartmentScope(userId, departmentId);
        return attendanceDao.findByDepartment(departmentId);
    }

    // Correct an entry. A reason is mandatory so there is an audit trail.
    @Override
    public AttendanceRecord correct(int attendanceId, LogAttendanceRequest r, int userId) {
        AttendanceRecord record = requireRecord(attendanceId);
        accessGuard.checkEmployeeScope(userId, record.getEmployeeId());
        accessGuard.checkNotSelf(userId, record.getEmployeeId(), "correct attendance");

        if (r.getOverrideReason() == null || r.getOverrideReason().isBlank()) {
            throw new IllegalArgumentException("overrideReason is required when correcting attendance");
        }

        applyFields(record, r);
        attendanceDao.update(record);

        notificationService.sendToEmployee(record.getEmployeeId(), "ATTENDANCE_CORRECTED",
                "Your attendance for " + record.getEventDate() + " was corrected to " + record.getAttendanceStatus()
                        + ". Reason: " + record.getOverrideReason());
        activityLogService.log(userId, "CORRECT_ATTENDANCE",
                "Corrected attendance #" + attendanceId + ": " + record.getOverrideReason());
        return requireRecord(attendanceId);
    }

    @Override
    public void delete(int attendanceId, int userId) {
        AttendanceRecord record = requireRecord(attendanceId);
        attendanceDao.deleteById(attendanceId);
        activityLogService.log(userId, "DELETE_ATTENDANCE",
                "Deleted attendance #" + attendanceId + " for employee #" + record.getEmployeeId() + " on " + record.getEventDate());
    }

    private void applyFields(AttendanceRecord record, LogAttendanceRequest r) {
        AttendanceStatus status = EnumUtils.parse(AttendanceStatus.class, r.getStatus(), "status");

        if (status == AttendanceStatus.ABSENT && (r.getCheckInTime() != null || r.getCheckOutTime() != null)) {
            throw new IllegalArgumentException("An ABSENT record cannot have check-in or check-out times");
        }
        if (r.getCheckInTime() != null && r.getCheckOutTime() != null && !r.getCheckOutTime().isAfter(r.getCheckInTime())) {
            throw new IllegalArgumentException("Check-out time must be after check-in time");
        }
        if (r.getCheckInTime() == null && r.getCheckOutTime() != null) {
            throw new IllegalArgumentException("Check-out time requires a check-in time");
        }
        if (status != AttendanceStatus.ABSENT && r.getCheckInTime() == null) {
            throw new IllegalArgumentException("A check-in time is required unless the employee was ABSENT");
        }

        record.setCheckInTime(r.getCheckInTime());
        record.setCheckOutTime(r.getCheckOutTime());
        record.setAttendanceStatus(status);
        record.setOverrideReason(r.getOverrideReason());
    }

    private boolean isOnApprovedLeave(int employeeId, LocalDate date) {
        return leaveRequestDao.findActiveOverlapping(employeeId, date, date).stream()
                .anyMatch(l -> l.getLeaveStatus() == LeaveStatus.APPROVED);
    }

    private Employee requireEmployee(int employeeId) {
        return employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
    }

    private AttendanceRecord requireRecord(int attendanceId) {
        return attendanceDao.findById(attendanceId)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance record not found: " + attendanceId));
    }
}
