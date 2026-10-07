package com.lankatech.ems.util;

import com.lankatech.ems.enums.LeaveType;
import com.lankatech.ems.model.LeaveRequest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaveBalanceCalculatorTest {

    @Test
    void leaveDaysIncludesBothEnds() {
        assertEquals(3, LeaveBalanceCalculator.leaveDays(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3)));
        assertEquals(1, LeaveBalanceCalculator.leaveDays(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1)));
    }

    @Test
    void rangeMustStartTodayOrLaterAndEndAfterStart() {
        LocalDate today = LocalDate.now();
        assertTrue(LeaveBalanceCalculator.isValidRange(today, today.plusDays(2)));
        assertFalse(LeaveBalanceCalculator.isValidRange(today.minusDays(1), today));
        assertFalse(LeaveBalanceCalculator.isValidRange(today.plusDays(3), today.plusDays(1)));
        assertFalse(LeaveBalanceCalculator.isValidRange(null, today));
    }

    @Test
    void remainingDaysNeverNegativeAndUnlimitedTypesReturnMinusOne() {
        assertEquals(11, LeaveBalanceCalculator.remainingDays(LeaveType.ANNUAL, 3));
        assertEquals(0, LeaveBalanceCalculator.remainingDays(LeaveType.CASUAL, 10));
        assertEquals(-1, LeaveBalanceCalculator.remainingDays(LeaveType.OTHER, 5));
    }

    @Test
    void polymorphicApplyEffectAndSyncedBaseFields() {
        LeaveRequest leave = new LeaveRequest();
        leave.setStartDate(LocalDate.of(2026, 10, 1));
        leave.setEndDate(LocalDate.of(2026, 10, 3));
        leave.setReason("Family event");

        // The inherited EmployeeEvent fields are kept in sync by the subclass setters
        assertEquals(LocalDate.of(2026, 10, 1), leave.getEventDate());
        assertEquals("Family event", leave.getNotes());
        // 1–3 Oct 2026 is Thu, Fri, Sat: the Saturday doesn't count
        assertEquals("Employee leave balance reduced by 2 day(s)", leave.applyEffect());
        assertEquals(3, leave.getCalendarDays());
    }

    @Test
    void workingDaysSkipWeekendsAndHolidays() {
        LocalDate mon = LocalDate.of(2026, 10, 5);
        LocalDate nextMon = LocalDate.of(2026, 10, 12);
        assertEquals(6, LeaveBalanceCalculator.workingDays(mon, nextMon, Set.of()));
        assertEquals(5, LeaveBalanceCalculator.workingDays(mon, nextMon, Set.of(LocalDate.of(2026, 10, 7))));
        assertEquals(0, LeaveBalanceCalculator.workingDays(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11), Set.of()));
    }

    @Test
    void requestOverNewYearIsSplitByYear() {
        // Mon 28 Dec 2026 – Fri 8 Jan 2027, with 25 Dec not in range
        LocalDate start = LocalDate.of(2026, 12, 28);
        LocalDate end = LocalDate.of(2027, 1, 8);
        assertEquals(4, LeaveBalanceCalculator.workingDaysInYear(start, end, 2026, Set.of()));
        assertEquals(6, LeaveBalanceCalculator.workingDaysInYear(start, end, 2027, Set.of()));
        assertEquals(0, LeaveBalanceCalculator.workingDaysInYear(start, end, 2025, Set.of()));
    }
}
