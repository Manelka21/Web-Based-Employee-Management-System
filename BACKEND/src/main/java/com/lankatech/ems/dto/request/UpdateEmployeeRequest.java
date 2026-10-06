package com.lankatech.ems.dto.request;

import com.lankatech.ems.util.ValidationRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

// Every field is optional: only the fields sent in the JSON are changed.
// NIC and hire date are not editable once the record exists.
public class UpdateEmployeeRequest {

    @Size(max = 80)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    private String firstName;

    @Size(max = 80)
    @Pattern(regexp = ValidationRules.NAME, message = ValidationRules.NAME_MESSAGE)
    private String lastName;

    @Size(max = 120)
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

    private String status;

    // MALE / FEMALE / OTHER
    private String gender;

    // A null department/position means "unchanged", so clearing needs an explicit flag.
    // Clearing the department also clears the position.
    private Boolean clearDepartment;
    private Boolean clearPosition;

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Boolean getClearDepartment() { return clearDepartment; }
    public void setClearDepartment(Boolean clearDepartment) { this.clearDepartment = clearDepartment; }

    public Boolean getClearPosition() { return clearPosition; }
    public void setClearPosition(Boolean clearPosition) { this.clearPosition = clearPosition; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

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

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
