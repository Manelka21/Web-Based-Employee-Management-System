package com.lankatech.ems.model;

import java.time.LocalDate;

// A company/public holiday. Leave requests don't count these days.
public class PublicHoliday {

    private LocalDate holidayDate;
    private String name;
    private Integer createdBy;

    public PublicHoliday() {
    }

    public PublicHoliday(LocalDate holidayDate, String name) {
        this.holidayDate = holidayDate;
        this.name = name;
    }

    public LocalDate getHolidayDate() { return holidayDate; }
    public void setHolidayDate(LocalDate holidayDate) { this.holidayDate = holidayDate; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getCreatedBy() { return createdBy; }
    public void setCreatedBy(Integer createdBy) { this.createdBy = createdBy; }
}
