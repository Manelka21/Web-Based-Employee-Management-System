package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.GeneratePayrollRequest;
import com.lankatech.ems.dto.request.SetSalaryRequest;
import com.lankatech.ems.dto.request.UpdatePayrollRequest;
import com.lankatech.ems.model.EmployeeSalary;
import com.lankatech.ems.model.PayrollRecord;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.PayrollService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @PreAuthorize("hasRole('PAYROLL_EXECUTIVE')")
    @PostMapping("/generate")
    public ResponseEntity<PayrollRecord> generate(@Valid @RequestBody GeneratePayrollRequest request,
                                                  @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(payrollService.generate(request, me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('PAYROLL_EXECUTIVE', 'COMPANY_DIRECTOR')")
    @GetMapping
    public ResponseEntity<List<PayrollRecord>> findAll() {
        return ResponseEntity.ok(payrollService.findAll());
    }

    // My own finalized/paid payslips
    @GetMapping("/my")
    public ResponseEntity<List<PayrollRecord>> myPayslips(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.findMine(me.getUserId()));
    }

    // Payroll Executive / Director: any employee; anyone else: only their own
    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<PayrollRecord>> findByEmployee(@PathVariable int empId,
                                                              @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.findByEmployee(empId, me.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PayrollRecord> findById(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.findById(id, me.getUserId()));
    }

    // Correct a DRAFT record
    @PreAuthorize("hasRole('PAYROLL_EXECUTIVE')")
    @PutMapping("/{id}")
    public ResponseEntity<PayrollRecord> update(@PathVariable int id, @Valid @RequestBody UpdatePayrollRequest request,
                                                @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.update(id, request, me.getUserId()));
    }

    @PreAuthorize("hasRole('PAYROLL_EXECUTIVE')")
    @PatchMapping("/{id}/finalize")
    public ResponseEntity<PayrollRecord> finalizeRecord(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.finalizeRecord(id, me.getUserId()));
    }

    @PreAuthorize("hasRole('PAYROLL_EXECUTIVE')")
    @PatchMapping("/{id}/pay")
    public ResponseEntity<PayrollRecord> markPaid(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.markPaid(id, me.getUserId()));
    }

    // ---------- salaries on file (HR and Payroll; supervisors never see pay) ----------

    @PreAuthorize("hasAnyRole('PAYROLL_EXECUTIVE', 'HR_MANAGER')")
    @GetMapping("/salaries")
    public ResponseEntity<List<EmployeeSalary>> salaries() {
        return ResponseEntity.ok(payrollService.findAllSalaries());
    }

    @PreAuthorize("hasAnyRole('PAYROLL_EXECUTIVE', 'HR_MANAGER')")
    @GetMapping("/salaries/{employeeId}")
    public ResponseEntity<EmployeeSalary> salary(@PathVariable int employeeId) {
        return ResponseEntity.ok(payrollService.findSalary(employeeId));
    }

    @PreAuthorize("hasAnyRole('PAYROLL_EXECUTIVE', 'HR_MANAGER')")
    @PutMapping("/salaries/{employeeId}")
    public ResponseEntity<EmployeeSalary> setSalary(@PathVariable int employeeId, @Valid @RequestBody SetSalaryRequest body,
                                                    @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.setSalary(employeeId, body.getBaseSalary(), me.getUserId()));
    }

    // Void (status -> VOIDED; the record is kept for audit)
    @PreAuthorize("hasRole('PAYROLL_EXECUTIVE')")
    @DeleteMapping("/{id}")
    public ResponseEntity<PayrollRecord> voidRecord(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(payrollService.voidRecord(id, me.getUserId()));
    }
}
