package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotNull;

public class EnrollTrainingRequest {

    // Ignored for self-enrolment (the logged-in employee is used instead)
    private Integer employeeId;

    @NotNull
    private Integer programId;

    public Integer getEmployeeId() { return employeeId; }
    public void setEmployeeId(Integer employeeId) { this.employeeId = employeeId; }

    public Integer getProgramId() { return programId; }
    public void setProgramId(Integer programId) { this.programId = programId; }
}
