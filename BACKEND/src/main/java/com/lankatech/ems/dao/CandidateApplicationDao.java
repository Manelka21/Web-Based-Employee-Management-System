package com.lankatech.ems.dao;

import com.lankatech.ems.model.CandidateApplication;
import java.util.List;
import java.util.Optional;

public interface CandidateApplicationDao {

    CandidateApplication save(CandidateApplication application);
    Optional<CandidateApplication> findById(int applicationId);
    List<CandidateApplication> findAll();
    List<CandidateApplication> findByVacancy(int vacancyId);
    void updateStatus(int applicationId, String status);
    void updateCandidate(CandidateApplication application);
    int countByStatus(String status);
}
