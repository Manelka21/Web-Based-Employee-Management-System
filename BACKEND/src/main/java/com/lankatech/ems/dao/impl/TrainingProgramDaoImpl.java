package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.TrainingProgramDao;
import com.lankatech.ems.dao.impl.rowmapper.TrainingProgramRowMapper;
import com.lankatech.ems.model.TrainingProgram;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainingProgramDaoImpl extends AbstractJdbcDao<TrainingProgram, Integer> implements TrainingProgramDao {

    private final TrainingProgramRowMapper mapper = new TrainingProgramRowMapper();

    public TrainingProgramDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public TrainingProgram save(TrainingProgram p) {
        String sql =
            "INSERT INTO training_programs (title, trainer, start_date, end_date, department_id, capacity, " +
            "description, status, created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            p.getTitle(), p.getTrainer(), p.getStartDate(), p.getEndDate(), p.getDepartmentId(),
            p.getCapacity(), p.getDescription(), p.getStatus().name(), p.getCreatedBy()
        );
        p.setProgramId(id);
        return p;
    }

    @Override
    public Optional<TrainingProgram> findById(int programId) {
        return queryOne("SELECT * FROM training_programs WHERE program_id = ?", mapper, programId);
    }

    @Override
    public List<TrainingProgram> findAll() {
        return queryList("SELECT * FROM training_programs ORDER BY start_date", mapper);
    }

    @Override
    public void update(TrainingProgram p) {
        String sql =
            "UPDATE training_programs SET title = ?, trainer = ?, start_date = ?, end_date = ?, " +
            "department_id = ?, capacity = ?, description = ?, status = ? WHERE program_id = ?";
        executeUpdate(sql,
            p.getTitle(), p.getTrainer(), p.getStartDate(), p.getEndDate(), p.getDepartmentId(),
            p.getCapacity(), p.getDescription(), p.getStatus().name(), p.getProgramId()
        );
    }

    @Override
    public void updateStatus(int programId, String status) {
        executeUpdate("UPDATE training_programs SET status = ? WHERE program_id = ?", status, programId);
    }

    @Override
    public void deleteById(int programId) {
        executeUpdate("DELETE FROM training_programs WHERE program_id = ?", programId);
    }
}
