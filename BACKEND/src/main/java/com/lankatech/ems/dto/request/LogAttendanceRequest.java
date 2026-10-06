package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;

public class LogAttendanceRequest {

    // Required when logging; ignored when correcting an existing entry
    private Integer employeeId;

    // Required when logging; ignored when correcting an existing entry
    private LocalDate date;

    private LocalTime checkInTime;
    private LocalTime checkOutTime;

    // PRESENT / ABSENT / LATE / HALF_DAY
    @NotNull
    private String status;

    // Required when correcting an existing entry
    @Size(max = 255)
    private String overrideReason;

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public LocalTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }

    public LocalTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
