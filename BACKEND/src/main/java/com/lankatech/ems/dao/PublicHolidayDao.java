package com.lankatech.ems.dao;

import com.lankatech.ems.model.PublicHoliday;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PublicHolidayDao {

    PublicHoliday save(PublicHoliday holiday);
    Optional<PublicHoliday> findByDate(LocalDate date);
    List<PublicHoliday> findBetween(LocalDate start, LocalDate end);
    void deleteByDate(LocalDate date);
}
