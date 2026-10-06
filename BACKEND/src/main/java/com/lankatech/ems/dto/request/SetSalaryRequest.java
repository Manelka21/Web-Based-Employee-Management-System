package com.lankatech.ems.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SetSalaryRequest {

    @NotNull
    @DecimalMin(value = "0.01", message = "Base salary must be greater than zero")
    @Digits(integer = 10, fraction = 2, message = "Base salary can have at most 10 digits and 2 decimals")
    private BigDecimal baseSalary;

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }
}
