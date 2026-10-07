package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateJobPositionRequest;
import com.lankatech.ems.model.JobPosition;

import java.util.List;

public interface JobPositionService {

    JobPosition create(CreateJobPositionRequest request, int actorUserId);
    List<JobPosition> findAll();
    List<JobPosition> findByDepartment(int departmentId);
    JobPosition findById(int positionId);
    JobPosition update(int positionId, CreateJobPositionRequest request, int actorUserId);
    void delete(int positionId, int actorUserId);
}
