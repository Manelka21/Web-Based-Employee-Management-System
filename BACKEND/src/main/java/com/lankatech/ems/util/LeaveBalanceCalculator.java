package com.lankatech.ems.util;

import com.lankatech.ems.enums.LeaveType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class LeaveBalanceCalculator {

    private LeaveBalanceCalculator() {
    }

    /**
     * Number of leave days between start and end, inclusive of both ends.
     * A same-day leave counts as 1.
     */
    public static long leaveDays(LocalDate startDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days < 1) {
            return 1;
        }
        return days;
    }

    /**
     * Leave days actually used: Saturdays, Sundays and public holidays don't count.
     */
    public static long workingDays(LocalDate startDate, LocalDate endDate, Set<LocalDate> holidays) {
        long count = 0;
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            if (isWorkingDay(d, holidays)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Working days of [startDate, endDate] that fall inside the given calendar year,
     * so a request spanning New Year is charged to each year correctly.
     */
    public static long workingDaysInYear(LocalDate startDate, LocalDate endDate, int year, Set<LocalDate> holidays) {
        LocalDate from = startDate.getYear() < year ? LocalDate.of(year, 1, 1) : startDate;
        LocalDate to = endDate.getYear() > year ? LocalDate.of(year, 12, 31) : endDate;
        if (from.isAfter(to)) {
            return 0;
        }
        return workingDays(from, to, holidays);
    }

    public static boolean isWorkingDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(date);
    }

    /**
     * Returns true if the date range is valid:
     *   - both non-null
     *   - start is today or later
     *   - end is on or after start
     */
    public static boolean isValidRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        if (startDate.isBefore(LocalDate.now())) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Days allowed per calendar year for each leave type.
     * -1 means "no fixed limit" (decided case by case by HR).
     */
    public static int annualEntitlement(LeaveType type) {
        switch (type) {
            case ANNUAL:
                return 14;
            case CASUAL:
                return 7;
            case SICK:
                return 7;
            case MATERNITY:
                return 84;
            default:
                return -1;
        }
    }

    /**
     * Remaining days = entitlement - days already used.
     * Returns -1 when the leave type has no fixed limit.
     */
    public static long remainingDays(LeaveType type, long usedDays) {
        int entitlement = annualEntitlement(type);
        if (entitlement < 0) {
            return -1;
        }
        return Math.max(0, entitlement - usedDays);
    }
}
