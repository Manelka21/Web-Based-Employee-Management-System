package com.lankatech.ems.model;

import com.lankatech.ems.enums.PayrollStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class PayrollRecord {

    private int payrollId;
    private int employeeId;
    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;
    private BigDecimal baseSalary;
    private BigDecimal overtimeHours;
    private BigDecimal overtimeAmount;
    private BigDecimal deductions;
    private BigDecimal netPay;
    private PayrollStatus status;
    private Integer generatedBy;               // user_id of the Payroll Executive
    private LocalDateTime generatedAt;

    public PayrollRecord() {
    }

    public int getPayrollId() { return payrollId; }
    public void setPayrollId(int payrollId) { this.payrollId = payrollId; }

    public int getEmployeeId() { return employeeId; }
    public void setEmployeeId(int employeeId) { this.employeeId = employeeId; }

    public LocalDate getPayPeriodStart() { return payPeriodStart; }
    public void setPayPeriodStart(LocalDate payPeriodStart) { this.payPeriodStart = payPeriodStart; }

    public LocalDate getPayPeriodEnd() { return payPeriodEnd; }
    public void setPayPeriodEnd(LocalDate payPeriodEnd) { this.payPeriodEnd = payPeriodEnd; }

    public BigDecimal getBaseSalary() { return baseSalary; }
    public void setBaseSalary(BigDecimal baseSalary) { this.baseSalary = baseSalary; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public BigDecimal getOvertimeAmount() { return overtimeAmount; }
    public void setOvertimeAmount(BigDecimal overtimeAmount) { this.overtimeAmount = overtimeAmount; }

    public BigDecimal getDeductions() { return deductions; }
    public void setDeductions(BigDecimal deductions) { this.deductions = deductions; }

    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    public PayrollStatus getStatus() { return status; }
    public void setStatus(PayrollStatus status) { this.status = status; }

    public Integer getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(Integer generatedBy) { this.generatedBy = generatedBy; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
}
