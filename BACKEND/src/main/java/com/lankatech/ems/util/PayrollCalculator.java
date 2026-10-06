package com.lankatech.ems.util;

import com.lankatech.ems.model.AttendanceRecord;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

public class PayrollCalculator {

    // 40 hours/week * 4 weeks
    private static final int STANDARD_MONTHLY_HOURS = 160;

    // 8 hours per working day
    private static final long STANDARD_DAY_MINUTES = 8 * 60;

    // Private constructor: this class is only a bag of static methods.
    private PayrollCalculator() {
    }

    /**
     * Overtime amount = overtime hours * (base salary / standard monthly hours) * 1.5
     * Standard monthly hours is assumed to be 160 (40 hours/week * 4 weeks).
     */
    public static BigDecimal overtimeAmount(BigDecimal baseSalary, BigDecimal overtimeHours) {
        if (overtimeHours == null || overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal hourlyRate = baseSalary.divide(BigDecimal.valueOf(STANDARD_MONTHLY_HOURS), 4, RoundingMode.HALF_UP);
        BigDecimal overtimeRate = hourlyRate.multiply(BigDecimal.valueOf(1.5));
        return overtimeRate.multiply(overtimeHours).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Net pay = base salary + overtime amount - deductions.
     * Returns zero if the result would be negative (shouldn't happen in practice).
     */
    public static BigDecimal netPay(BigDecimal baseSalary, BigDecimal overtimeAmount, BigDecimal deductions) {
        BigDecimal net = baseSalary.add(overtimeAmount).subtract(deductions);
        if (net.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return net.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Overtime hours taken from attendance: every minute worked beyond
     * 8 hours on a day counts as overtime. Records without both a
     * check-in and a check-out time are skipped.
     */
    public static BigDecimal overtimeHoursFromAttendance(List<AttendanceRecord> records) {
        long overtimeMinutes = 0;

        for (AttendanceRecord record : records) {
            if (record.getCheckInTime() == null || record.getCheckOutTime() == null) {
                continue;
            }
            long workedMinutes = Duration.between(record.getCheckInTime(), record.getCheckOutTime()).toMinutes();
            if (workedMinutes > STANDARD_DAY_MINUTES) {
                overtimeMinutes += workedMinutes - STANDARD_DAY_MINUTES;
            }
        }

        return BigDecimal.valueOf(overtimeMinutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
