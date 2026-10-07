package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.VacancyDao;
import com.lankatech.ems.dao.impl.rowmapper.VacancyRowMapper;
import com.lankatech.ems.model.Vacancy;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class VacancyDaoImpl extends AbstractJdbcDao<Vacancy, Integer> implements VacancyDao {

    private final VacancyRowMapper mapper = new VacancyRowMapper();

    public VacancyDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public Vacancy save(Vacancy v) {
        String sql =
            "INSERT INTO vacancies (title, department_id, requirements, deadline, status, created_by) " +
            "VALUES (?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            v.getTitle(), v.getDepartmentId(), v.getRequirements(), v.getDeadline(),
            v.getStatus().name(), v.getCreatedBy()
        );
        v.setVacancyId(id);
        return v;
    }

    @Override
    public Optional<Vacancy> findById(int vacancyId) {
        return queryOne("SELECT * FROM vacancies WHERE vacancy_id = ?", mapper, vacancyId);
    }

    @Override
    public List<Vacancy> findAll() {
        return queryList("SELECT * FROM vacancies ORDER BY vacancy_id DESC", mapper);
    }

    @Override
    public List<Vacancy> findByStatus(String status) {
        return queryList("SELECT * FROM vacancies WHERE status = ? ORDER BY vacancy_id DESC", mapper, status);
    }

    @Override
    public void update(Vacancy v) {
        String sql = "UPDATE vacancies SET title = ?, department_id = ?, requirements = ?, deadline = ? WHERE vacancy_id = ?";
        executeUpdate(sql, v.getTitle(), v.getDepartmentId(), v.getRequirements(), v.getDeadline(), v.getVacancyId());
    }

    @Override
    public void updateStatus(int vacancyId, String status) {
        executeUpdate("UPDATE vacancies SET status = ? WHERE vacancy_id = ?", status, vacancyId);
    }

    @Override
    public void deleteById(int vacancyId) {
        executeUpdate("DELETE FROM vacancies WHERE vacancy_id = ?", vacancyId);
    }

    @Override
    public int countByStatus(String status) {
        return queryForInt("SELECT COUNT(*) FROM vacancies WHERE status = ?", status);
    }

    @Override
    public int countOpenByDepartment(int departmentId) {
        return queryForInt("SELECT COUNT(*) FROM vacancies WHERE department_id = ? AND status = 'OPEN'", departmentId);
    }
}
