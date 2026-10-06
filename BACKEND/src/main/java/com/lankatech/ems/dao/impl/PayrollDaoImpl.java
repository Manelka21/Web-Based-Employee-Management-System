package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.PayrollDao;
import com.lankatech.ems.dao.impl.rowmapper.PayrollRowMapper;
import com.lankatech.ems.model.PayrollRecord;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PayrollDaoImpl extends AbstractJdbcDao<PayrollRecord, Integer> implements PayrollDao {

    private final PayrollRowMapper mapper = new PayrollRowMapper();

    public PayrollDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public PayrollRecord save(PayrollRecord p) {
        String sql =
            "INSERT INTO payroll_records (employee_id, pay_period_start, pay_period_end, base_salary, overtime_hours, " +
            "overtime_amount, deductions, net_pay, status, generated_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            p.getEmployeeId(), p.getPayPeriodStart(), p.getPayPeriodEnd(), p.getBaseSalary(), p.getOvertimeHours(),
            p.getOvertimeAmount(), p.getDeductions(), p.getNetPay(), p.getStatus().name(), p.getGeneratedBy()
        );
        p.setPayrollId(id);
        return p;
    }

    @Override
    public Optional<PayrollRecord> findById(int payrollId) {
        return queryOne("SELECT * FROM payroll_records WHERE payroll_id = ?", mapper, payrollId);
    }

    @Override
    public List<PayrollRecord> findAll() {
        return queryList("SELECT * FROM payroll_records ORDER BY pay_period_start DESC, employee_id", mapper);
    }

    @Override
    public List<PayrollRecord> findByEmployee(int employeeId) {
        return queryList("SELECT * FROM payroll_records WHERE employee_id = ? ORDER BY pay_period_start DESC", mapper, employeeId);
    }

    @Override
    public List<PayrollRecord> findActiveOverlapping(int employeeId, LocalDate start, LocalDate end) {
        String sql =
            "SELECT * FROM payroll_records " +
            "WHERE employee_id = ? AND status <> 'VOIDED' AND pay_period_start <= ? AND pay_period_end >= ?";
        return queryList(sql, mapper, employeeId, end, start);
    }

    @Override
    public void update(PayrollRecord p) {
        String sql =
            "UPDATE payroll_records SET base_salary = ?, overtime_hours = ?, overtime_amount = ?, deductions = ?, " +
            "net_pay = ? WHERE payroll_id = ?";
        executeUpdate(sql,
            p.getBaseSalary(), p.getOvertimeHours(), p.getOvertimeAmount(), p.getDeductions(), p.getNetPay(), p.getPayrollId()
        );
    }

    @Override
    public void updateStatus(int payrollId, String status) {
        executeUpdate("UPDATE payroll_records SET status = ? WHERE payroll_id = ?", status, payrollId);
    }

    @Override
    public int countByStatus(String status) {
        return queryForInt("SELECT COUNT(*) FROM payroll_records WHERE status = ?", status);
    }
}
