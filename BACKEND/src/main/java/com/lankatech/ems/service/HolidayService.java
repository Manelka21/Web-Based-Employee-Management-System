package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateHolidayRequest;
import com.lankatech.ems.model.PublicHoliday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface HolidayService {

    List<PublicHoliday> findByYear(int year);
    PublicHoliday add(CreateHolidayRequest request, int userId);
    void delete(LocalDate date, int userId);

    // Holiday dates between start and end (inclusive), for leave-day counting
    Set<LocalDate> datesBetween(LocalDate start, LocalDate end);
}
