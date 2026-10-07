package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.CandidateApplicationDao;
import com.lankatech.ems.dao.impl.rowmapper.CandidateApplicationRowMapper;
import com.lankatech.ems.model.CandidateApplication;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class CandidateApplicationDaoImpl extends AbstractJdbcDao<CandidateApplication, Integer>
        implements CandidateApplicationDao {

    private final CandidateApplicationRowMapper mapper = new CandidateApplicationRowMapper();

    public CandidateApplicationDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public CandidateApplication save(CandidateApplication a) {
        String sql =
            "INSERT INTO candidate_applications (vacancy_id, candidate_name, candidate_email, candidate_phone, " +
            "candidate_nic, resume_notes, status, applied_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            a.getVacancyId(), a.getCandidateName(), a.getCandidateEmail(), a.getCandidatePhone(),
            a.getCandidateNic(), a.getResumeNotes(), a.getStatus().name(), a.getAppliedDate()
        );
        a.setApplicationId(id);
        return a;
    }

    @Override
    public Optional<CandidateApplication> findById(int applicationId) {
        return queryOne("SELECT * FROM candidate_applications WHERE application_id = ?", mapper, applicationId);
    }

    @Override
    public List<CandidateApplication> findAll() {
        return queryList("SELECT * FROM candidate_applications ORDER BY application_id DESC", mapper);
    }

    @Override
    public List<CandidateApplication> findByVacancy(int vacancyId) {
        return queryList("SELECT * FROM candidate_applications WHERE vacancy_id = ? ORDER BY applied_date", mapper, vacancyId);
    }

    @Override
    public void updateStatus(int applicationId, String status) {
        executeUpdate("UPDATE candidate_applications SET status = ? WHERE application_id = ?", status, applicationId);
    }

    @Override
    public void updateCandidate(CandidateApplication a) {
        String sql =
            "UPDATE candidate_applications SET candidate_name = ?, candidate_email = ?, candidate_phone = ?, " +
            "candidate_nic = ?, resume_notes = ? WHERE application_id = ?";
        executeUpdate(sql, a.getCandidateName(), a.getCandidateEmail(), a.getCandidatePhone(),
                a.getCandidateNic(), a.getResumeNotes(), a.getApplicationId());
    }

    @Override
    public int countByStatus(String status) {
        return queryForInt("SELECT COUNT(*) FROM candidate_applications WHERE status = ?", status);
    }
}
