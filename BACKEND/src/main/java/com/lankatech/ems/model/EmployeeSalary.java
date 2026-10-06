package com.lankatech.ems.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// The monthly base salary on file for an employee. Kept in its own table so
// salary never appears in ordinary employee records seen by supervisors.
public class EmployeeSalary {

    private int employeeId;
    private BigDecimal baseSalary;
    private Integer updatedBy;
    private LocalDateTime updatedAt;

    public EmployeeSalary() {
    }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public Integer getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Integer updatedBy) { this.updatedBy = updatedBy; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
