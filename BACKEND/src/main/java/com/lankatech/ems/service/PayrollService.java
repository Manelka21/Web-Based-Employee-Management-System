package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.GeneratePayrollRequest;
import com.lankatech.ems.dto.request.UpdatePayrollRequest;
import com.lankatech.ems.model.EmployeeSalary;
import com.lankatech.ems.model.PayrollRecord;

import java.util.List;

public interface PayrollService {

    PayrollRecord generate(GeneratePayrollRequest request, int userId);
    List<PayrollRecord> findAll();
    PayrollRecord findById(int payrollId, int userId);
    List<PayrollRecord> findByEmployee(int employeeId, int userId);
    List<PayrollRecord> findMine(int userId);
    PayrollRecord update(int payrollId, UpdatePayrollRequest request, int userId);
    PayrollRecord finalizeRecord(int payrollId, int userId);
    PayrollRecord markPaid(int payrollId, int userId);
    PayrollRecord voidRecord(int payrollId, int userId);

    // ---------- salaries on file ----------
    List<EmployeeSalary> findAllSalaries();
    EmployeeSalary findSalary(int employeeId);
    EmployeeSalary setSalary(int employeeId, java.math.BigDecimal baseSalary, int userId);
}
