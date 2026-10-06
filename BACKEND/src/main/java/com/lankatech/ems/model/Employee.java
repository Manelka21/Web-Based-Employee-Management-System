package com.lankatech.ems.model;

import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Gender;
import java.time.LocalDate;
import java.time.LocalDateTime;

// The managed employee record — separate from the user account.
public class Employee {

    private int employeeId;
    private String firstName;
    private String lastName;
    private String nic;
    private String email;
    private String phone;
    private String address;
    private Integer departmentId;
    private Integer positionId;
    private LocalDate hireDate;
    private EmployeeStatus status;
    private Gender gender;                 // nullable; needed for maternity leave
    private LocalDateTime createdAt;

    public Employee() {
    }

    // Derived attribute: years of service since hire date.
    public int getYearsOfService() {
        if (hireDate == null) {
            return 0;
        }
        return LocalDate.now().getYear() - hireDate.getYear();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

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

    public EmployeeStatus getStatus() { return status; }
    public void setStatus(EmployeeStatus status) { this.status = status; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
