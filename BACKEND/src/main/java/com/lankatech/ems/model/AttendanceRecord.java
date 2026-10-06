package com.lankatech.ems.model;

import com.lankatech.ems.enums.AttendanceStatus;
import java.time.LocalTime;

public class AttendanceRecord extends EmployeeEvent {

    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private AttendanceStatus attendanceStatus;   // PRESENT / ABSENT / LATE / HALF_DAY
    private String overrideReason;

    public AttendanceRecord() {
    }

    @Override
    public String applyEffect() {
        // A completed attendance record marks the employee as accounted-for that day.
        return "Employee attendance recorded for the day";
    }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public AttendanceStatus getAttendanceStatus() { return attendanceStatus; }

    // Keeps the inherited String status in sync with the enum.
    public void setAttendanceStatus(AttendanceStatus attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
        setStatus(attendanceStatus != null ? attendanceStatus.name() : null);
    }

    public String getOverrideReason() { return overrideReason; }

    // Keeps the inherited notes in sync with the override reason.
    public void setOverrideReason(String overrideReason) {
        this.overrideReason = overrideReason;
        setNotes(overrideReason);
    }
}
