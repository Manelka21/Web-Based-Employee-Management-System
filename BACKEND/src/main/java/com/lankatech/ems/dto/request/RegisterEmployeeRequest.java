package com.lankatech.ems.dto.request;

import com.lankatech.ems.util.ValidationRules;
import jakarta.validation.constraints.*;

import java.time.LocalDate;

public class RegisterEmployeeRequest {

    @NotBlank @Size(max = 80)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    private String firstName;

    @NotBlank @Size(max = 80)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    private String lastName;

    @NotBlank
    @Pattern(regexp = ValidationRules.NIC, message = ValidationRules.NIC_MESSAGE)
    private String nic;

    @NotBlank @Size(max = 120)
    @Pattern(regexp = ValidationRules.EMAIL, message = ValidationRules.EMAIL_MESSAGE)
    private String email;

    @Size(max = 20)
    @Pattern(regexp = ValidationRules.PHONE, message = ValidationRules.PHONE_MESSAGE)
    private String phone;

    @Size(max = 255)
    private String address;

    @Positive
    private Integer departmentId;

    @Positive
    private Integer positionId;

    @NotNull
    private LocalDate hireDate;

    // Optional: ACTIVE / INACTIVE / ON_LEAVE / PROBATION (defaults to ACTIVE)
    private String status;

    // Optional: MALE / FEMALE / OTHER (needed before maternity leave can be requested)
    private String gender;

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getNic() { return nic; }
    public void setNic(String nic) { this.nic = nic; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Integer getDepartmentId() { return departmentId; }
    public void setDepartmentId(Integer departmentId) { this.departmentId = departmentId; }

    public Integer getPositionId() { return positionId; }
    public void setPositionId(Integer positionId) { this.positionId = positionId; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
