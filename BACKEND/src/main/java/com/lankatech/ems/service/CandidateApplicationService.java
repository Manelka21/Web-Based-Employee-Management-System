package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateApplicationRequest;
import com.lankatech.ems.dto.request.UpdateApplicationRequest;
import com.lankatech.ems.dto.response.ApplicationStatusResponse;
import com.lankatech.ems.model.CandidateApplication;

import java.util.List;

public interface CandidateApplicationService {

    CandidateApplication create(CreateApplicationRequest request, int userId);
    List<CandidateApplication> findAll();
    CandidateApplication findById(int applicationId);
    List<CandidateApplication> findByVacancy(int vacancyId);
    ApplicationStatusResponse updateStatus(int applicationId, String status, int userId);
    CandidateApplication withdraw(int applicationId, int userId);
    CandidateApplication update(int applicationId, UpdateApplicationRequest request, int userId);
}
