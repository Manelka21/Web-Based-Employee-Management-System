package com.lankatech.ems.model;

import java.time.LocalDate;

// Abstract superclass shared by AttendanceRecord and LeaveRequest.
// Both are "a dated event tied to an employee, with a status" — that's what
// this base class captures.
public abstract class EmployeeEvent {

    private int eventId;          // attendance_id or leave_id
    private int employeeId;
    private LocalDate eventDate;  // attendance date or leave start date
    private String status;
    private String notes;         // override reason or leave reason

    protected EmployeeEvent() {
    }

    // Polymorphism (overriding): each subclass explains what "resolving"
    // this event means for the employee's records.
    public abstract String applyEffect();

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
