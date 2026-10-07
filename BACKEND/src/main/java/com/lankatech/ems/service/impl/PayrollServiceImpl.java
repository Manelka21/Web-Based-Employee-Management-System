package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.*;
import com.lankatech.ems.dto.request.GeneratePayrollRequest;
import com.lankatech.ems.dto.request.UpdatePayrollRequest;
import com.lankatech.ems.enums.PayrollStatus;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.*;
import com.lankatech.ems.model.*;
import com.lankatech.ems.security.AccessGuard;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.service.PayrollService;
import com.lankatech.ems.util.PayrollCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PayrollServiceImpl implements PayrollService {

    private static final int MAX_PERIOD_DAYS = 31;

    private final PayrollDao payrollDao;
    private final EmployeeDao employeeDao;
    private final LeaveRequestDao leaveRequestDao;
    private final AttendanceDao attendanceDao;
    private final EmployeeSalaryDao salaryDao;
    private final AccessGuard accessGuard;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public PayrollServiceImpl(PayrollDao payrollDao, EmployeeDao employeeDao, LeaveRequestDao leaveRequestDao,
                              AttendanceDao attendanceDao, EmployeeSalaryDao salaryDao, AccessGuard accessGuard,
                              NotificationService notificationService, ActivityLogService activityLogService) {
        this.payrollDao = payrollDao;
        this.employeeDao = employeeDao;
        this.leaveRequestDao = leaveRequestDao;
        this.attendanceDao = attendanceDao;
        this.salaryDao = salaryDao;
        this.accessGuard = accessGuard;
        this.notificationService = notificationService;
        this.activityLogService = activityLogService;
    }

    @Override
    @Transactional
    public PayrollRecord generate(GeneratePayrollRequest r, int userId) {
        LocalDate periodStart = r.getPayPeriodStart();
        LocalDate periodEnd = r.getPayPeriodEnd();
        int employeeId = r.getEmployeeId();

        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Pay period end cannot be before pay period start");
        }

        if (ChronoUnit.DAYS.between(periodStart, periodEnd) + 1 > MAX_PERIOD_DAYS) {
            throw new IllegalArgumentException("A pay period cannot be longer than " + MAX_PERIOD_DAYS + " days");
        }

        Employee employee = employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        accessGuard.checkNotSelf(userId, employeeId, "generate payroll");

        if (employee.getHireDate() != null && periodEnd.isBefore(employee.getHireDate())) {
            throw new IllegalArgumentException(employee.getFullName() + " was hired on " + employee.getHireDate()
                    + ", after this pay period ends");
        }

        // One active payroll record per employee per period
        if (!payrollDao.findActiveOverlapping(employeeId, periodStart, periodEnd).isEmpty()) {
            throw new DuplicateRecordException("A payroll record already exists for employee #" + employeeId
                    + " overlapping " + periodStart + " to " + periodEnd + ". Void it first to regenerate.");
        }

        // Business rule: attendance/leave must be finalized for the pay period.
        // "Finalized" means no PENDING leave requests exist for that employee in that period.
        List<LeaveRequest> pendingLeaves = leaveRequestDao.findPendingForEmployeeInPeriod(employeeId, periodStart, periodEnd);
        if (!pendingLeaves.isEmpty()) {
            throw new PayrollNotReadyException(
                "Cannot generate payroll — " + pendingLeaves.size() +
                " pending leave request(s) exist for this employee in the period " +
                periodStart + " to " + periodEnd);
        }

        // Attendance for the period: used for overtime when the request doesn't give overtime hours
        List<AttendanceRecord> records = attendanceDao.findByEmployeeAndPeriod(employeeId, periodStart, periodEnd);

        // Base salary: the amount typed in, otherwise the salary on file
        BigDecimal baseSalary = r.getBaseSalary();
        if (baseSalary == null) {
            baseSalary = salaryDao.findByEmployee(employeeId)
                    .map(EmployeeSalary::getBaseSalary)
                    .orElseThrow(() -> new IllegalArgumentException(employee.getFullName()
                            + " has no salary on file. Enter a base salary, or set it under Payroll → Salaries."));
        }

        BigDecimal overtimeHours = r.getOvertimeHours() != null
                ? r.getOvertimeHours()
                : PayrollCalculator.overtimeHoursFromAttendance(records);
        BigDecimal deductions = r.getDeductions() != null ? r.getDeductions() : BigDecimal.ZERO;
        BigDecimal overtimeAmount = PayrollCalculator.overtimeAmount(baseSalary, overtimeHours);
        checkDeductions(baseSalary, overtimeAmount, deductions);
        BigDecimal netPay = PayrollCalculator.netPay(baseSalary, overtimeAmount, deductions);

        PayrollRecord payroll = new PayrollRecord();
        payroll.setEmployeeId(employeeId);
        payroll.setPayPeriodStart(periodStart);
        payroll.setPayPeriodEnd(periodEnd);
        payroll.setBaseSalary(baseSalary);
        payroll.setOvertimeHours(overtimeHours);
        payroll.setOvertimeAmount(overtimeAmount);
        payroll.setDeductions(deductions);
        payroll.setNetPay(netPay);
        payroll.setStatus(PayrollStatus.DRAFT);
        payroll.setGeneratedBy(userId);

        PayrollRecord saved = payrollDao.save(payroll);
        activityLogService.log(userId, "GENERATE_PAYROLL",
                "Generated payroll #" + saved.getPayrollId() + " for employee #" + employeeId + " (" + employee.getFullName()
                        + ") period " + periodStart + "–" + periodEnd + ", " + records.size() + " attendance record(s), net pay " + netPay);
        return requirePayroll(saved.getPayrollId());
    }

    @Override
    public List<PayrollRecord> findAll() {
        return payrollDao.findAll();
    }

    @Override
    public PayrollRecord findById(int payrollId, int userId) {
        PayrollRecord record = requirePayroll(payrollId);
        if (!canSeeAllPayroll(userId)) {
            ensureOwnPayslip(record, userId);
        }
        return record;
    }

    // Payroll Executive / Director see everything; anyone else only their own finalized payslips.
    @Override
    public List<PayrollRecord> findByEmployee(int employeeId, int userId) {
        if (canSeeAllPayroll(userId)) {
            return payrollDao.findByEmployee(employeeId);
        }
        if (accessGuard.requireEmployeeId(userId) != employeeId) {
            throw new UnauthorizedActionException("You can only view your own payroll records");
        }
        return onlyReleased(payrollDao.findByEmployee(employeeId));
    }

    @Override
    public List<PayrollRecord> findMine(int userId) {
        return onlyReleased(payrollDao.findByEmployee(accessGuard.requireEmployeeId(userId)));
    }

    // Correct a DRAFT record; overtime amount and net pay are recalculated.
    @Override
    public PayrollRecord update(int payrollId, UpdatePayrollRequest r, int userId) {
        PayrollRecord record = requirePayroll(payrollId);
        if (record.getStatus() != PayrollStatus.DRAFT) {
            throw new InvalidStatusTransitionException("Only DRAFT payroll records can be corrected, current status: " + record.getStatus());
        }
        accessGuard.checkNotSelf(userId, record.getEmployeeId(), "correct payroll");

        if (r.getBaseSalary() != null) {
            record.setBaseSalary(r.getBaseSalary());
        }
        if (r.getOvertimeHours() != null) {
            record.setOvertimeHours(r.getOvertimeHours());
        }
        if (r.getDeductions() != null) {
            record.setDeductions(r.getDeductions());
        }

        BigDecimal deductions = record.getDeductions() != null ? record.getDeductions() : BigDecimal.ZERO;
        record.setOvertimeAmount(PayrollCalculator.overtimeAmount(record.getBaseSalary(), record.getOvertimeHours()));
        checkDeductions(record.getBaseSalary(), record.getOvertimeAmount(), deductions);
        record.setNetPay(PayrollCalculator.netPay(record.getBaseSalary(), record.getOvertimeAmount(), deductions));

        payrollDao.update(record);
        activityLogService.log(userId, "UPDATE_PAYROLL", "Corrected payroll #" + payrollId + ", new net pay " + record.getNetPay());
        return requirePayroll(payrollId);
    }

    // DRAFT -> FINALIZED (the employee can now see the payslip)
    @Override
    public PayrollRecord finalizeRecord(int payrollId, int userId) {
        PayrollRecord record = changeStatus(payrollId, PayrollStatus.DRAFT, PayrollStatus.FINALIZED, userId);
        notificationService.sendToEmployee(record.getEmployeeId(), "PAYSLIP_AVAILABLE",
                "Your payslip for " + record.getPayPeriodStart() + " to " + record.getPayPeriodEnd()
                        + " is available. Net pay: " + record.getNetPay());
        return record;
    }

    // FINALIZED -> PAID
    @Override
    public PayrollRecord markPaid(int payrollId, int userId) {
        PayrollRecord record = changeStatus(payrollId, PayrollStatus.FINALIZED, PayrollStatus.PAID, userId);
        notificationService.sendToEmployee(record.getEmployeeId(), "SALARY_PAID",
                "Your salary for " + record.getPayPeriodStart() + " to " + record.getPayPeriodEnd() + " has been paid");
        return record;
    }

    // DRAFT or FINALIZED -> VOIDED (a PAID record can never be voided)
    @Override
    public PayrollRecord voidRecord(int payrollId, int userId) {
        PayrollRecord record = requirePayroll(payrollId);
        if (record.getStatus() == PayrollStatus.PAID || record.getStatus() == PayrollStatus.VOIDED) {
            throw new InvalidStatusTransitionException("A " + record.getStatus() + " payroll record cannot be voided");
        }
        payrollDao.updateStatus(payrollId, PayrollStatus.VOIDED.name());
        activityLogService.log(userId, "VOID_PAYROLL", "Voided payroll #" + payrollId + " (was " + record.getStatus() + ")");
        return requirePayroll(payrollId);
    }

    // ---------- salaries on file ----------

    @Override
    public List<EmployeeSalary> findAllSalaries() {
        return salaryDao.findAll();
    }

    @Override
    public EmployeeSalary findSalary(int employeeId) {
        employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        return salaryDao.findByEmployee(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("No salary on file for employee #" + employeeId));
    }

    // HR or the Payroll Executive records the salary. Nobody sets their own pay.
    @Override
    public EmployeeSalary setSalary(int employeeId, BigDecimal baseSalary, int userId) {
        Employee employee = employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        accessGuard.checkNotSelf(userId, employeeId, "set the salary");
        if (employee.getStatus() == com.lankatech.ems.enums.EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException(employee.getFullName() + " is inactive");
        }

        String before = salaryDao.findByEmployee(employeeId).map(s -> s.getBaseSalary().toPlainString()).orElse("none");
        salaryDao.upsert(employeeId, baseSalary, userId);
        activityLogService.log(userId, "SET_SALARY",
                "Salary of employee #" + employeeId + " changed from " + before + " to " + baseSalary.toPlainString());
        return findSalary(employeeId);
    }

    // ---------- helpers ----------

    private PayrollRecord changeStatus(int payrollId, PayrollStatus expected, PayrollStatus next, int userId) {
        PayrollRecord record = requirePayroll(payrollId);
        accessGuard.checkNotSelf(userId, record.getEmployeeId(), "process payroll");
        if (record.getStatus() != expected) {
            throw new InvalidStatusTransitionException("Payroll #" + payrollId + " must be " + expected
                    + " to become " + next + ", current status: " + record.getStatus());
        }
        payrollDao.updateStatus(payrollId, next.name());
        activityLogService.log(userId, next.name() + "_PAYROLL", "Payroll #" + payrollId + " changed from " + expected + " to " + next);
        return requirePayroll(payrollId);
    }

    // Business rule: deductions can't be more than gross pay. Previously net pay was
    // silently clamped to 0, hiding data-entry mistakes.
    private void checkDeductions(BigDecimal baseSalary, BigDecimal overtimeAmount, BigDecimal deductions) {
        BigDecimal gross = baseSalary.add(overtimeAmount);
        if (deductions.compareTo(gross) > 0) {
            throw new IllegalArgumentException("Deductions (" + deductions + ") cannot be more than gross pay (" + gross + ")");
        }
    }

    private boolean canSeeAllPayroll(int userId) {
        User user = accessGuard.currentUser(userId);
        return user.getRole() == Role.PAYROLL_EXECUTIVE || user.getRole() == Role.COMPANY_DIRECTOR;
    }

    private void ensureOwnPayslip(PayrollRecord record, int userId) {
        boolean own = accessGuard.requireEmployeeId(userId) == record.getEmployeeId();
        boolean released = record.getStatus() == PayrollStatus.FINALIZED || record.getStatus() == PayrollStatus.PAID;
        if (!own || !released) {
            throw new UnauthorizedActionException("You can only view your own finalized payslips");
        }
    }

    // Employees don't see DRAFT (still being prepared) or VOIDED records
    private List<PayrollRecord> onlyReleased(List<PayrollRecord> records) {
        return records.stream()
                .filter(p -> p.getStatus() == PayrollStatus.FINALIZED || p.getStatus() == PayrollStatus.PAID)
                .collect(Collectors.toList());
    }

    private PayrollRecord requirePayroll(int payrollId) {
        return payrollDao.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found: " + payrollId));
    }
}
