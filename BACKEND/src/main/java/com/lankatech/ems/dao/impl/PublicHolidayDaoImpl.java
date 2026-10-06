package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.PublicHolidayDao;
import com.lankatech.ems.dao.impl.rowmapper.PublicHolidayRowMapper;
import com.lankatech.ems.model.PublicHoliday;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class PublicHolidayDaoImpl extends AbstractJdbcDao<PublicHoliday, LocalDate> implements PublicHolidayDao {

    private final PublicHolidayRowMapper mapper = new PublicHolidayRowMapper();

    public PublicHolidayDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public PublicHoliday save(PublicHoliday h) {
        executeUpdate("INSERT INTO public_holidays (holiday_date, name, created_by) VALUES (?, ?, ?)",
                h.getHolidayDate(), h.getName(), h.getCreatedBy());
        return h;
    }

    @Override
    public Optional<PublicHoliday> findByDate(LocalDate date) {
        return queryOne("SELECT * FROM public_holidays WHERE holiday_date = ?", mapper, date);
    }

    @Override
    public List<PublicHoliday> findBetween(LocalDate start, LocalDate end) {
        return queryList("SELECT * FROM public_holidays WHERE holiday_date BETWEEN ? AND ? ORDER BY holiday_date", mapper, start, end);
    }

    @Override
    public void deleteByDate(LocalDate date) {
        executeUpdate("DELETE FROM public_holidays WHERE holiday_date = ?", date);
    }
}
