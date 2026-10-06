package com.lankatech.ems.model;

import com.lankatech.ems.enums.EnrollmentStatus;
import java.time.LocalDate;

public class TrainingEnrollment {

    private int enrollmentId;
    private int employeeId;
    private int programId;
    private LocalDate enrolledDate;
    private EnrollmentStatus completionStatus;
    private LocalDate completionDate;

    public TrainingEnrollment() {
    }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public int getProgramId() { return programId; }
    public void setProgramId(int programId) { this.programId = programId; }

    public LocalDate getEnrolledDate() { return enrolledDate; }
    public void setEnrolledDate(LocalDate enrolledDate) { this.enrolledDate = enrolledDate; }

    public EnrollmentStatus getCompletionStatus() { return completionStatus; }
    public void setCompletionStatus(EnrollmentStatus completionStatus) { this.completionStatus = completionStatus; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }
}
