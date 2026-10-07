package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateTrainingProgramRequest;
import com.lankatech.ems.model.TrainingEnrollment;
import com.lankatech.ems.model.TrainingProgram;

import java.util.List;

public interface TrainingService {

    // ---------- programs ----------
    TrainingProgram createProgram(CreateTrainingProgramRequest request, int userId);
    List<TrainingProgram> findAllPrograms();
    TrainingProgram findProgram(int programId);
    TrainingProgram updateProgram(int programId, CreateTrainingProgramRequest request, int userId);
    TrainingProgram cancelProgram(int programId, int userId);
    void deleteProgram(int programId, int userId);

    // ---------- enrollments ----------
    TrainingEnrollment enrol(int employeeId, int programId, int userId);
    TrainingEnrollment selfEnrol(int programId, int userId);
    List<TrainingEnrollment> findByEmployee(int employeeId, int userId);
    List<TrainingEnrollment> findMine(int userId);
    List<TrainingEnrollment> findByProgram(int programId);
    TrainingEnrollment complete(int enrollmentId, int userId);
    TrainingEnrollment drop(int enrollmentId, int userId);
}
