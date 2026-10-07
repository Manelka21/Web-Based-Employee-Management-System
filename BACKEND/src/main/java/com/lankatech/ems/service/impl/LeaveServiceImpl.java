package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.LeaveRequestDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.dto.request.ApplyLeaveRequest;
import com.lankatech.ems.enums.Gender;
import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.enums.LeaveType;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.InvalidStatusTransitionException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.exception.UnauthorizedActionException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.LeaveRequest;
import com.lankatech.ems.model.User;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.HolidayService;
import com.lankatech.ems.service.LeaveService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.util.EnumUtils;
import com.lankatech.ems.util.LeaveBalanceCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final int MAX_DAYS_PER_REQUEST = 90;   // calendar days

    private final LeaveRequestDao leaveRequestDao;
    private final UserDao userDao;
    private final AccessGuard accessGuard;
    private final HolidayService holidayService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public LeaveServiceImpl(LeaveRequestDao leaveRequestDao, UserDao userDao, AccessGuard accessGuard,
                            HolidayService holidayService, NotificationService notificationService,
                            ActivityLogService activityLogService) {
        this.leaveRequestDao = leaveRequestDao;
        this.userDao = userDao;
        this.accessGuard = accessGuard;
        this.holidayService = holidayService;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Override
    public LeaveRequest apply(ApplyLeaveRequest r, int userId) {
        Employee me = accessGuard.requireLinkedEmployee(userId);
        int employeeId = me.getEmployeeId();

        if (r.getEmployeeId() != null && r.getEmployeeId() != employeeId) {
            throw new UnauthorizedActionException("You can only apply for leave for yourself");
        }

        LeaveType type = EnumUtils.parse(LeaveType.class, r.getLeaveType(), "leaveType");

        // Business rule: maternity leave is for female employees (gender is recorded by HR)
        if (type == LeaveType.MATERNITY && me.getGender() != Gender.FEMALE) {
            throw new IllegalArgumentException(me.getGender() == null
                    ? "HR hasn't recorded your gender yet, which maternity leave requires. Contact HR."
                    : "Maternity leave is only available to female employees");
        }

        if (!LeaveBalanceCalculator.isValidRange(r.getStartDate(), r.getEndDate())) {
            throw new IllegalArgumentException("Invalid dates: start date must be today or later and end date must not be before start date");
        }

        if (!leaveRequestDao.findActiveOverlapping(employeeId, r.getStartDate(), r.getEndDate()).isEmpty()) {
            throw new DuplicateRecordException("You already have a pending or approved leave request overlapping these dates");
        }

        // Sanity limits: book at most a year ahead, at most 90 calendar days in one request
        if (r.getStartDate().isAfter(LocalDate.now().plusYears(1))) {
            throw new IllegalArgumentException("Leave can be requested at most one year in advance");
        }
        if (LeaveBalanceCalculator.leaveDays(r.getStartDate(), r.getEndDate()) > MAX_DAYS_PER_REQUEST) {
            throw new IllegalArgumentException("A single leave request cannot be longer than " + MAX_DAYS_PER_REQUEST + " days");
        }
        if (me.getHireDate() != null && r.getStartDate().isBefore(me.getHireDate())) {
            throw new IllegalArgumentException("Leave cannot start before your hire date (" + me.getHireDate() + ")");
        }

        // Business rule: weekends and public holidays don't use up leave
        Set<LocalDate> holidays = holidayService.datesBetween(r.getStartDate(), r.getEndDate());
        long days = LeaveBalanceCalculator.workingDays(r.getStartDate(), r.getEndDate(), holidays);
        if (days == 0) {
            throw new IllegalArgumentException("The chosen dates are all weekends or public holidays, so no leave is needed");
        }

        // Balance check per calendar year the request touches (pending requests also count,
        // so employees can't over-book). A request over New Year is charged to both years.
        int entitlement = LeaveBalanceCalculator.annualEntitlement(type);
        if (entitlement >= 0) {
            for (int year = r.getStartDate().getYear(); year <= r.getEndDate().getYear(); year++) {
                long requestedThisYear = LeaveBalanceCalculator.workingDaysInYear(r.getStartDate(), r.getEndDate(), year, holidays);
                long remaining = LeaveBalanceCalculator.remainingDays(type, usedDays(employeeId, type, year));
                if (requestedThisYear > remaining) {
                    throw new IllegalArgumentException("Not enough " + type + " leave in " + year + ": requested "
                            + requestedThisYear + " working day(s), remaining " + remaining);
                }
            }
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(type);
        leave.setStartDate(r.getStartDate());
        leave.setEndDate(r.getEndDate());
        leave.setReason(r.getReason());

        // Business rule: leave requests always start as PENDING.
        // They must be explicitly approved by a Supervisor or HR Manager.
        // There is no auto-approval path.
        leave.setLeaveStatus(LeaveStatus.PENDING);

        LeaveRequest saved = leaveRequestDao.save(leave);

        notifyApprovers(me, userId, me.getFullName() + " requested " + days + " working day(s) of " + type
                + " leave from " + r.getStartDate() + " to " + r.getEndDate() + " (request #" + saved.getEventId() + ")");
        activityLogService.log(userId, "APPLY_LEAVE",
                "Leave request #" + saved.getEventId() + " for employee #" + employeeId + ": " + type + " " + days + " working day(s)");
        return findLeave(saved.getEventId());
    }

    @Override
    public List<LeaveRequest> findMine(int userId) {
        return withWorkingDays(leaveRequestDao.findByEmployee(accessGuard.requireEmployeeId(userId)));
    }

    @Override
    public List<LeaveRequest> findByEmployee(int employeeId, int userId) {
        accessGuard.checkEmployeeScope(userId, employeeId);
        return withWorkingDays(leaveRequestDao.findByEmployee(employeeId));
    }

    // HR sees every pending request; a supervisor sees their own department's.
    @Override
    public List<LeaveRequest> findPending(int userId) {
        User user = accessGuard.currentUser(userId);
        if (user.getRole() == Role.DEPT_SUPERVISOR) {
            return withWorkingDays(leaveRequestDao.findPendingByDepartment(accessGuard.requireSupervisorDepartment(userId)));
        }
        return withWorkingDays(leaveRequestDao.findByStatus(LeaveStatus.PENDING.name()));
    }

    @Override
    public LeaveRequest findById(int leaveId, int userId) {
        LeaveRequest leave = findLeave(leaveId);
        accessGuard.checkEmployeeScope(userId, leave.getEmployeeId());
        return leave;
    }

    @Override
    @Transactional
    public LeaveRequest approve(int leaveId, int userId, String comment) {
        return decide(leaveId, userId, comment, LeaveStatus.APPROVED);
    }

    @Override
    @Transactional
    public LeaveRequest reject(int leaveId, int userId, String comment) {
        return decide(leaveId, userId, comment, LeaveStatus.REJECTED);
    }

    // The employee cancels their own request (pending, or approved but not started yet).
    @Override
    public LeaveRequest cancel(int leaveId, int userId) {
        LeaveRequest leave = findLeave(leaveId);
        int myEmployeeId = accessGuard.requireEmployeeId(userId);

        if (leave.getEmployeeId() != myEmployeeId) {
            throw new UnauthorizedActionException("You can only cancel your own leave requests");
        }

        boolean pending = leave.getLeaveStatus() == LeaveStatus.PENDING;
        boolean approvedFuture = leave.getLeaveStatus() == LeaveStatus.APPROVED && leave.getStartDate().isAfter(LocalDate.now());
        if (!pending && !approvedFuture) {
            throw new InvalidStatusTransitionException("Only PENDING requests, or APPROVED requests that have not started, can be cancelled. Current status: "
                    + leave.getLeaveStatus());
        }

        leave.setLeaveStatus(LeaveStatus.CANCELLED);
        leaveRequestDao.updateStatus(leave);
        activityLogService.log(userId, "CANCEL_LEAVE", "Cancelled leave request #" + leaveId);
        return findLeave(leaveId);
    }

    // Leave balance for the current year, one row per leave type (working days)
    @Override
    public List<Map<String, Object>> balance(int userId) {
        int employeeId = accessGuard.requireEmployeeId(userId);
        int year = LocalDate.now().getYear();
        Set<LocalDate> holidays = holidayService.datesBetween(LocalDate.of(year - 1, 1, 1), LocalDate.of(year + 1, 12, 31));
        List<Map<String, Object>> rows = new ArrayList<>();

        for (LeaveType type : LeaveType.values()) {
            long approvedDays = 0;
            long pendingDays = 0;
            for (LeaveRequest l : leaveRequestDao.findActiveByEmployeeTypeOverlappingYear(employeeId, type.name(), year)) {
                long days = LeaveBalanceCalculator.workingDaysInYear(l.getStartDate(), l.getEndDate(), year, holidays);
                if (l.getLeaveStatus() == LeaveStatus.APPROVED) {
                    approvedDays += days;
                } else {
                    pendingDays += days;
                }
            }

            int entitlement = LeaveBalanceCalculator.annualEntitlement(type);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("leaveType", type.name());
            row.put("year", year);
            row.put("entitlement", entitlement < 0 ? null : entitlement);
            row.put("approvedDays", approvedDays);
            row.put("pendingDays", pendingDays);
            row.put("remainingDays", entitlement < 0 ? null : LeaveBalanceCalculator.remainingDays(type, approvedDays + pendingDays));
            rows.add(row);
        }
        return rows;
    }

    // ---------- helpers ----------

    private LeaveRequest decide(int leaveId, int userId, String comment, LeaveStatus decision) {
        LeaveRequest leave = findLeave(leaveId);

        if (leave.getLeaveStatus() != LeaveStatus.PENDING) {
            throw new InvalidStatusTransitionException(
                "Can only " + (decision == LeaveStatus.APPROVED ? "approve" : "reject")
                        + " a PENDING leave request, current status: " + leave.getLeaveStatus());
        }

        User approver = accessGuard.currentUser(userId);
        if (approver.getEmployeeId() != null && approver.getEmployeeId() == leave.getEmployeeId()) {
            throw new UnauthorizedActionException("You cannot approve or reject your own leave request");
        }
        // Supervisors may only decide for their own team
        accessGuard.checkEmployeeScope(userId, leave.getEmployeeId());

        leave.setLeaveStatus(decision);
        leave.setApprovedBy(userId);
        leave.setApprovedDate(LocalDateTime.now());
        leaveRequestDao.updateStatus(leave);

        String message = "Your " + leave.getLeaveType() + " leave request from " + leave.getStartDate() + " to "
                + leave.getEndDate() + " has been " + decision.name().toLowerCase() + " by " + approver.getFullName() + "."
                + (comment != null && !comment.isBlank() ? " Comment: " + comment : "");
        notificationService.sendToEmployee(leave.getEmployeeId(), "LEAVE_" + decision.name(), message);

        String details = decision.name() + " leave request #" + leaveId + " for employee #" + leave.getEmployeeId();
        if (decision == LeaveStatus.APPROVED) {
            // Polymorphism: applyEffect() is LeaveRequest's version here
            details += ": " + leave.applyEffect();
        }
        activityLogService.log(userId, decision == LeaveStatus.APPROVED ? "APPROVE_LEAVE" : "REJECT_LEAVE", details);

        return findLeave(leaveId);
    }

    // Tell the employee's department supervisors; if there are none, tell HR.
    private void notifyApprovers(Employee employee, int applicantUserId, String message) {
        boolean notified = false;
        if (employee.getDepartmentId() != null) {
            for (User supervisor : userDao.findSupervisorsByDepartment(employee.getDepartmentId())) {
                if (supervisor.getUserId() != applicantUserId) {
                    notificationService.send(supervisor.getUserId(), "LEAVE_REQUESTED", message);
                    notified = true;
                }
            }
        }
        if (!notified) {
            notificationService.sendToRole(Role.HR_MANAGER, "LEAVE_REQUESTED", message);
        }
    }

    // Working days of this type already used (approved + pending) inside one calendar year
    private long usedDays(int employeeId, LeaveType type, int year) {
        Set<LocalDate> holidays = holidayService.datesBetween(LocalDate.of(year - 1, 1, 1), LocalDate.of(year + 1, 12, 31));
        long used = 0;
        for (LeaveRequest l : leaveRequestDao.findActiveByEmployeeTypeOverlappingYear(employeeId, type.name(), year)) {
            used += LeaveBalanceCalculator.workingDaysInYear(l.getStartDate(), l.getEndDate(), year, holidays);
        }
        return used;
    }

    // Fill in the holiday-aware working-day count shown as "leaveDays" in the API
    private List<LeaveRequest> withWorkingDays(List<LeaveRequest> leaves) {
        if (leaves.isEmpty()) {
            return leaves;
        }
        LocalDate min = leaves.stream().map(LeaveRequest::getStartDate).min(LocalDate::compareTo).orElseThrow();
        LocalDate max = leaves.stream().map(LeaveRequest::getEndDate).max(LocalDate::compareTo).orElseThrow();
        Set<LocalDate> holidays = holidayService.datesBetween(min, max);
        for (LeaveRequest l : leaves) {
            l.setWorkingDays(LeaveBalanceCalculator.workingDays(l.getStartDate(), l.getEndDate(), holidays));
        }
        return leaves;
    }

    private LeaveRequest findLeave(int leaveId) {
        LeaveRequest leave = leaveRequestDao.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found: " + leaveId));
        return withWorkingDays(new ArrayList<>(List.of(leave))).get(0);
    }
}
