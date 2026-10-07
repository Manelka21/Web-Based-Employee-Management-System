package com.lankatech.ems.dao;

import com.lankatech.ems.model.TrainingEnrollment;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingEnrollmentDao {

    TrainingEnrollment save(TrainingEnrollment enrollment);
    Optional<TrainingEnrollment> findById(int enrollmentId);
    Optional<TrainingEnrollment> findByEmployeeAndProgram(int employeeId, int programId);
    List<TrainingEnrollment> findByEmployee(int employeeId);
    List<TrainingEnrollment> findByProgram(int programId);
    void updateStatus(int enrollmentId, String status, LocalDate completionDate);
    void reEnroll(int enrollmentId, LocalDate enrolledDate);
    int countActiveByProgram(int programId);
}
