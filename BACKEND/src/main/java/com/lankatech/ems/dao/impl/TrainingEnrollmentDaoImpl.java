package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.TrainingEnrollmentDao;
import com.lankatech.ems.dao.impl.rowmapper.TrainingEnrollmentRowMapper;
import com.lankatech.ems.model.TrainingEnrollment;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class TrainingEnrollmentDaoImpl extends AbstractJdbcDao<TrainingEnrollment, Integer>
        implements TrainingEnrollmentDao {

    private final TrainingEnrollmentRowMapper mapper = new TrainingEnrollmentRowMapper();

    public TrainingEnrollmentDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public TrainingEnrollment save(TrainingEnrollment en) {
        String sql =
            "INSERT INTO training_enrollments (employee_id, program_id, enrolled_date, completion_status) " +
            "VALUES (?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            en.getEmployeeId(), en.getProgramId(), en.getEnrolledDate(), en.getCompletionStatus().name()
        );
        en.setEnrollmentId(id);
        return en;
    }

    @Override
    public Optional<TrainingEnrollment> findById(int enrollmentId) {
        return queryOne("SELECT * FROM training_enrollments WHERE enrollment_id = ?", mapper, enrollmentId);
    }

    @Override
    public Optional<TrainingEnrollment> findByEmployeeAndProgram(int employeeId, int programId) {
        String sql = "SELECT * FROM training_enrollments WHERE employee_id = ? AND program_id = ?";
        return queryOne(sql, mapper, employeeId, programId);
    }

    @Override
    public List<TrainingEnrollment> findByEmployee(int employeeId) {
        return queryList("SELECT * FROM training_enrollments WHERE employee_id = ? ORDER BY enrolled_date DESC", mapper, employeeId);
    }

    @Override
    public List<TrainingEnrollment> findByProgram(int programId) {
        return queryList("SELECT * FROM training_enrollments WHERE program_id = ? ORDER BY enrolled_date", mapper, programId);
    }

    @Override
    public void updateStatus(int enrollmentId, String status, LocalDate completionDate) {
        String sql = "UPDATE training_enrollments SET completion_status = ?, completion_date = ? WHERE enrollment_id = ?";
        executeUpdate(sql, status, completionDate, enrollmentId);
    }

    @Override
    public void reEnroll(int enrollmentId, LocalDate enrolledDate) {
        String sql =
            "UPDATE training_enrollments SET completion_status = 'ENROLLED', enrolled_date = ?, completion_date = NULL " +
            "WHERE enrollment_id = ?";
        executeUpdate(sql, enrolledDate, enrollmentId);
    }

    @Override
    public int countActiveByProgram(int programId) {
        // Dropped enrollments don't use up a seat.
        String sql = "SELECT COUNT(*) FROM training_enrollments WHERE program_id = ? AND completion_status <> 'DROPPED'";
        return queryForInt(sql, programId);
    }
}
