package com.lankatech.ems.model;

import com.lankatech.ems.enums.LeaveStatus;
import com.lankatech.ems.enums.LeaveType;
import com.lankatech.ems.util.LeaveBalanceCalculator;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LeaveRequest extends EmployeeEvent {

    private LeaveType leaveType;                // ANNUAL / SICK / CASUAL / MATERNITY / OTHER
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private LeaveStatus leaveStatus;            // PENDING / APPROVED / REJECTED / CANCELLED
    private Integer approvedBy;                 // user_id of the approver
    private LocalDateTime approvedDate;
    private LocalDateTime submittedDate;

    public LeaveRequest() {
    }

    @Override
    public String applyEffect() {
        // An approved leave request reduces the employee's leave balance.
        return "Employee leave balance reduced by " + getLeaveDays() + " day(s)";
    }

    // Working days (weekends and public holidays excluded). Filled in by LeaveServiceImpl,
    // which knows the holiday calendar; not stored in the database.
    private Long workingDays;

    // Derived attribute: number of leave days this request uses. Falls back to
    // weekdays only when the service hasn't supplied the holiday-aware figure.
    public long getLeaveDays() {
        if (workingDays != null) {
            return workingDays;
        }
        if (startDate == null || endDate == null) {
            return 0;
        }
        return LeaveBalanceCalculator.workingDays(startDate, endDate, java.util.Set.of());
    }

    public void setWorkingDays(Long workingDays) { this.workingDays = workingDays; }

    // Calendar days, weekends included (for display)
    public long getCalendarDays() {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return LeaveBalanceCalculator.leaveDays(startDate, endDate);
    }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }

    // Keeps the inherited eventDate in sync with the start date.
    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        setEventDate(startDate);
    }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public String getReason() { return reason; }

    // Keeps the inherited notes in sync with the reason.
    public void setReason(String reason) {
        this.reason = reason;
        setNotes(reason);
    }

    public LeaveStatus getLeaveStatus() { return leaveStatus; }

    // Keeps the inherited String status in sync with the enum.
    public void setLeaveStatus(LeaveStatus leaveStatus) {
        this.leaveStatus = leaveStatus;
        setStatus(leaveStatus != null ? leaveStatus.name() : null);
    }

    public Integer getApprovedBy() { return approvedBy; }
    public void setApprovedBy(Integer approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedDate() { return approvedDate; }
    public void setApprovedDate(LocalDateTime approvedDate) { this.approvedDate = approvedDate; }

    public LocalDateTime getSubmittedDate() { return submittedDate; }
    public void setSubmittedDate(LocalDateTime submittedDate) { this.submittedDate = submittedDate; }
}
