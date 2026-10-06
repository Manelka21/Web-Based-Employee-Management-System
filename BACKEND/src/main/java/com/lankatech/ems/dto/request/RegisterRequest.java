package com.lankatech.ems.dto.request;

import com.lankatech.ems.util.ValidationRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public self-registration. Only people HR already has an employee record for can
 * register: the work email AND the NIC must match that record. Names come from the
 * employee record, so they aren't asked for here.
 */
public class RegisterRequest {

    @NotBlank @Size(max = 120)
    @Pattern(regexp = ValidationRules.EMAIL, message = ValidationRules.EMAIL_MESSAGE)
    private String email;

    @NotBlank
    @Pattern(regexp = ValidationRules.NIC, message = ValidationRules.NIC_MESSAGE)
    private String nic;

    @NotBlank
    @Pattern(regexp = ValidationRules.PASSWORD, message = ValidationRules.PASSWORD_MESSAGE)
    private String password;

    @Size(max = 20)
    @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE)
    private String phoneNumber;

    // Optional: a management role to request. The account still starts as EMPLOYEE;
    // the IT Administrator is notified and grants the role.
    @Size(max = 30)
    private String role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
