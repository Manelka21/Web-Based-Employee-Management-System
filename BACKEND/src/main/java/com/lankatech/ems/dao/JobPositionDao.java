package com.lankatech.ems.dao;

import com.lankatech.ems.model.JobPosition;
import java.util.List;
import java.util.Optional;

public interface JobPositionDao {

    JobPosition save(JobPosition position);
    Optional<JobPosition> findById(int positionId);
    List<JobPosition> findAll();
    List<JobPosition> findByDepartment(int departmentId);
    void update(JobPosition position);
    void deleteById(int positionId);
}
