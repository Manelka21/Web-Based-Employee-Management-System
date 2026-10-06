package com.lankatech.ems.dto.response;

import com.lankatech.ems.model.Employee;

// Result of changing an application's status.
// createdEmployee is only filled in when the candidate was HIRED.
public class ApplicationStatusResponse {

    private CandidateApplication application;
    private Employee createdEmployee;

    public ApplicationStatusResponse() {}

    public ApplicationStatusResponse(CandidateApplication application, Employee createdEmployee) {
        this.application = application;
        this.createdEmployee = createdEmployee;
    }

    public CandidateApplication getApplication() { return application; }
    public void setApplication(CandidateApplication application) { this.application = application; }

    public Employee getCreatedEmployee() { return createdEmployee; }
    public void setCreatedEmployee(Employee createdEmployee) { this.createdEmployee = createdEmployee; }
}
