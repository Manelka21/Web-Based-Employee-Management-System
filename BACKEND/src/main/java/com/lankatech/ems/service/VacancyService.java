package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateVacancyRequest;
import com.lankatech.ems.model.Vacancy;

import java.util.List;

public interface VacancyService {

    Vacancy create(CreateVacancyRequest request, int userId);
    List<Vacancy> findAll(String status);
    Vacancy findById(int vacancyId);
    Vacancy update(int vacancyId, CreateVacancyRequest request, int userId);
    Vacancy changeStatus(int vacancyId, String status, int userId);
    Vacancy close(int vacancyId, int userId);
    void delete(int vacancyId, int userId);
}
