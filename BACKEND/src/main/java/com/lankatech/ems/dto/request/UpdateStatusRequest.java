package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotBlank;

// Generic body for PATCH .../status endpoints: { "status": "SHORTLISTED" }
public class UpdateStatusRequest {

    @NotBlank
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
