package com.lankatech.ems.dao;

import com.lankatech.ems.model.TrainingProgram;
import java.util.List;
import java.util.Optional;

public interface TrainingProgramDao {

    TrainingProgram save(TrainingProgram program);
    Optional<TrainingProgram> findById(int programId);
    List<TrainingProgram> findAll();
    void update(TrainingProgram program);
    void updateStatus(int programId, String status);
    void deleteById(int programId);
}
