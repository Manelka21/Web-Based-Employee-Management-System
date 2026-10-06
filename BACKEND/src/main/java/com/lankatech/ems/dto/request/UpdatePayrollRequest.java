package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;

import java.math.BigDecimal;

// Correct a DRAFT payroll record. Only the fields sent are changed;
// overtime amount and net pay are always recalculated.
public class UpdatePayrollRequest {

    @DecimalMin(value = "0.01", message = "Base salary must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Base salary can have at most 10 digits and 2 decimals")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0.00", message = "Overtime hours cannot be negative")
    @DecimalMax(value = "200.00", message = "Overtime hours cannot be more than 200 in one pay period")
    @Digits(integer = 3, fraction = 2, message = "Overtime hours can have at most 2 decimals")
    private BigDecimal overtimeHours;

    @DecimalMin(value = "0.00", message = "Deductions cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Deductions can have at most 10 digits and 2 decimals")
    private BigDecimal deductions;

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }
}
