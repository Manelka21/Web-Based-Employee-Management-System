package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.PublicHolidayDao;
import com.lankatech.ems.dto.request.CreateHolidayRequest;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.PublicHoliday;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.HolidayService;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HolidayServiceImpl implements HolidayService {

    private final PublicHolidayDao holidayDao;
    private final ActivityLogService activityLogService;

    public HolidayServiceImpl(PublicHolidayDao holidayDao, ActivityLogService activityLogService) {
        this.holidayDao = holidayDao;
        this.activityLogService = activityLogService;
    }

    @Override
    public List<PublicHoliday> findByYear(int year) {
        return holidayDao.findBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    @Override
    public PublicHoliday add(CreateHolidayRequest r, int userId) {
        LocalDate date = r.getHolidayDate();
        if (date.isBefore(LocalDate.now().withDayOfYear(1))) {
            throw new IllegalArgumentException("Holidays can only be added for this year or later");
        }
        if (date.isAfter(LocalDate.now().plusYears(2))) {
            throw new IllegalArgumentException("Holidays can be added at most two years ahead");
        }
        if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            throw new IllegalArgumentException(date + " is a weekend, which never counts as a leave day anyway");
        }
        if (holidayDao.findByDate(date).isPresent()) {
            throw new DuplicateRecordException(date + " is already a holiday");
        }

        PublicHoliday holiday = new PublicHoliday(date, r.getName().trim());
        holiday.setCreatedBy(userId);
        holidayDao.save(holiday);
        activityLogService.log(userId, "ADD_HOLIDAY", "Added holiday " + date + " (" + holiday.getName() + ")");
        return holiday;
    }

    @Override
    public void delete(LocalDate date, int userId) {
        PublicHoliday holiday = holidayDao.findByDate(date)
                .orElseThrow(() -> new ResourceNotFoundException("No holiday on " + date));
        holidayDao.deleteByDate(date);
        activityLogService.log(userId, "DELETE_HOLIDAY", "Removed holiday " + date + " (" + holiday.getName() + ")");
    }

    @Override
    public Set<LocalDate> datesBetween(LocalDate start, LocalDate end) {
        return holidayDao.findBetween(start, end).stream()
                .map(PublicHoliday::getHolidayDate)
                .collect(Collectors.toSet());
    }
}
