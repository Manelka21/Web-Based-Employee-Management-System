package com.lankatech.ems.dto.request;

import com.lankatech.ems.util.ValidationRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Self-service: an employee may only change their own contact details.
public class UpdateProfileRequest {

    @Size(max = 20)
    @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE)
    private String phone;

    @Size(max = 255)
    private String address;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}
