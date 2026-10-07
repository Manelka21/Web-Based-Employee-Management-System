package com.lankatech.ems.dao;

import com.lankatech.ems.model.Vacancy;
import java.util.List;
import java.util.Optional;

public interface VacancyDao {

    Vacancy save(Vacancy vacancy);
    Optional<Vacancy> findById(int vacancyId);
    List<Vacancy> findAll();
    List<Vacancy> findByStatus(String status);
    void update(Vacancy vacancy);
    void updateStatus(int vacancyId, String status);
    void deleteById(int vacancyId);
    int countByStatus(String status);
    int countOpenByDepartment(int departmentId);
}
