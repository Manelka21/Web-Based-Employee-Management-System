package com.lankatech.ems.util;

import com.lankatech.ems.model.AttendanceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Plain unit tests: no Spring context and no database needed.
class PayrollCalculatorTest {

    @Test
    void overtimeAmountUsesTimeAndAHalf() {
        // 75000 / 160 = 468.75 per hour, * 1.5 = 703.125, * 10 hours = 7031.25
        BigDecimal amount = PayrollCalculator.overtimeAmount(new BigDecimal("75000"), new BigDecimal("10"));
        assertEquals(new BigDecimal("7031.25"), amount);
    }

    @Test
    void noOvertimeHoursMeansZeroAmount() {
        assertEquals(new BigDecimal("0.00"), PayrollCalculator.overtimeAmount(new BigDecimal("75000"), null));
        assertEquals(new BigDecimal("0.00"), PayrollCalculator.overtimeAmount(new BigDecimal("75000"), BigDecimal.ZERO));
    }

    @Test
    void netPayAddsOvertimeAndSubtractsDeductions() {
        BigDecimal net = PayrollCalculator.netPay(new BigDecimal("75000"), new BigDecimal("7031.25"), new BigDecimal("5000"));
        assertEquals(new BigDecimal("77031.25"), net);
    }

    @Test
    void netPayNeverNegative() {
        BigDecimal net = PayrollCalculator.netPay(new BigDecimal("1000"), BigDecimal.ZERO, new BigDecimal("5000"));
        assertEquals(new BigDecimal("0.00"), net);
    }

    @Test
    void overtimeHoursCountOnlyTimeBeyondEightHours() {
        AttendanceRecord longDay = new AttendanceRecord();
        longDay.setCheckInTime(LocalTime.of(8, 45));
        longDay.setCheckOutTime(LocalTime.of(17, 30));   // 8h45m -> 45 minutes overtime

        AttendanceRecord shortDay = new AttendanceRecord();
        shortDay.setCheckInTime(LocalTime.of(9, 0));
        shortDay.setCheckOutTime(LocalTime.of(13, 0));   // 4h -> no overtime

        AttendanceRecord noCheckOut = new AttendanceRecord();
        noCheckOut.setCheckInTime(LocalTime.of(9, 0));   // skipped

        BigDecimal hours = PayrollCalculator.overtimeHoursFromAttendance(List.of(longDay, shortDay, noCheckOut));
        assertEquals(new BigDecimal("0.75"), hours);
    }
}
