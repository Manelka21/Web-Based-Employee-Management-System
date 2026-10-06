package com.lankatech.ems.dao.impl.rowmapper;

import com.lankatech.ems.dao.RowMapper;
import com.lankatech.ems.enums.PayrollStatus;
import com.lankatech.ems.model.PayrollRecord;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PayrollRowMapper implements RowMapper<PayrollRecord> {

    @Override
    public PayrollRecord map(ResultSet rs) throws SQLException {
        PayrollRecord p = new PayrollRecord();
        p.setPayrollId(rs.getInt("payroll_id"));
        p.setEmployeeId(rs.getInt("employee_id"));

        Date start = rs.getDate("pay_period_start");
        if (start != null) p.setPayPeriodStart(start.toLocalDate());

        Date end = rs.getDate("pay_period_end");
        if (end != null) p.setPayPeriodEnd(end.toLocalDate());

        p.setBaseSalary(rs.getBigDecimal("base_salary"));
        p.setOvertimeHours(rs.getBigDecimal("overtime_hours"));
        p.setOvertimeAmount(rs.getBigDecimal("overtime_amount"));
        p.setDeductions(rs.getBigDecimal("deductions"));
        p.setNetPay(rs.getBigDecimal("net_pay"));
        p.setStatus(PayrollStatus.valueOf(rs.getString("status")));

        int generatedBy = rs.getInt("generated_by");
        if (!rs.wasNull()) p.setGeneratedBy(generatedBy);

        Timestamp generated = rs.getTimestamp("generated_at");
        if (generated != null) p.setGeneratedAt(generated.toLocalDateTime());

        return p;
    }
}
