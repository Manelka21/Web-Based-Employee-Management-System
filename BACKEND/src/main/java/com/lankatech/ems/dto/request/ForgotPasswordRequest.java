package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {

    @NotBlank @Size(max = 120)
    private String email;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
