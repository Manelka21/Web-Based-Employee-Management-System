package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.LeaveRequestDao;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.DataAccessException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.LeaveRequest;
import com.lankatech.ems.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Daily housekeeping for leave (runs at start-up and every day at 00:05):
 *
 *  1. Employee status follows approved leave: ACTIVE employees on approved leave today
 *     become ON_LEAVE, and ON_LEAVE employees with no approved leave today go back to
 *     ACTIVE. (PROBATION and INACTIVE are never changed, so probation isn't lost.)
 *
 *  2. Stale requests expire: a request still PENDING more than EXPIRY_DAYS after its
 *     start date is CANCELLED, the employee and HR are told, and it stops blocking payroll.
 */
@Component
public class LeaveStatusJob {

    static final int EXPIRY_DAYS = 7;

    private static final Logger log = LoggerFactory.getLogger(LeaveStatusJob.class);

    private final EmployeeDao employeeDao;
    private final LeaveRequestDao leaveRequestDao;
    private final NotificationService notificationService;

    public LeaveStatusJob(EmployeeDao employeeDao, LeaveRequestDao leaveRequestDao, NotificationService notificationService) {
        this.employeeDao = employeeDao;
        this.leaveRequestDao = leaveRequestDao;
        this.notificationService = notificationService;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 5 0 * * *")
    public void run() {
        try {
            LocalDate today = LocalDate.now();
            int statusChanges = syncOnLeaveStatus(today);
            int expired = expireStalePending(today);
            if (statusChanges > 0 || expired > 0) {
                log.info("Leave job: {} employee status change(s), {} stale request(s) cancelled", statusChanges, expired);
            }
        } catch (DataAccessException e) {
            log.warn("Leave job skipped: {}", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    int syncOnLeaveStatus(LocalDate today) {
        Set<Integer> onLeaveToday = leaveRequestDao.findApprovedCovering(today).stream()
                .map(LeaveRequest::getEmployeeId)
                .collect(Collectors.toSet());

        int changes = 0;
        for (Employee e : employeeDao.findAll()) {
            if (e.getStatus() == EmployeeStatus.ACTIVE && onLeaveToday.contains(e.getEmployeeId())) {
                employeeDao.updateStatus(e.getEmployeeId(), EmployeeStatus.ON_LEAVE.name());
                changes++;
            } else if (e.getStatus() == EmployeeStatus.ON_LEAVE && !onLeaveToday.contains(e.getEmployeeId())) {
                employeeDao.updateStatus(e.getEmployeeId(), EmployeeStatus.ACTIVE.name());
                changes++;
            }
        }
        return changes;
    }

    int expireStalePending(LocalDate today) {
        List<LeaveRequest> stale = leaveRequestDao.findPendingStartedBefore(today.minusDays(EXPIRY_DAYS));
        for (LeaveRequest leave : stale) {
            leave.setLeaveStatus(LeaveStatus.CANCELLED);
            leaveRequestDao.updateStatus(leave);
            notificationService.sendToEmployee(leave.getEmployeeId(), "LEAVE_EXPIRED",
                    "Your " + leave.getLeaveType() + " leave request for " + leave.getStartDate() + " to " + leave.getEndDate()
                            + " was never decided and has been cancelled automatically. Please talk to your supervisor.");
        }
        if (!stale.isEmpty()) {
            notificationService.sendToRole(Role.HR_MANAGER, "LEAVE_EXPIRED",
                    stale.size() + " leave request(s) were still pending " + EXPIRY_DAYS
                            + " days after their start date and have been cancelled automatically.");
        }
        return stale.size();
    }
}
