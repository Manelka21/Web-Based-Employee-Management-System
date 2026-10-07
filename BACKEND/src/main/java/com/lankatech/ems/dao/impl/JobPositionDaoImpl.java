package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.JobPositionDao;
import com.lankatech.ems.dao.impl.rowmapper.JobPositionRowMapper;
import com.lankatech.ems.model.JobPosition;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class JobPositionDaoImpl extends AbstractJdbcDao<JobPosition, Integer> implements JobPositionDao {

    private final JobPositionRowMapper mapper = new JobPositionRowMapper();

    public JobPositionDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public JobPosition save(JobPosition p) {
        String sql = "INSERT INTO job_positions (title, department_id, description) VALUES (?, ?, ?)";
        int id = executeInsertReturnId(sql, p.getTitle(), p.getDepartmentId(), p.getDescription());
        p.setPositionId(id);
        return p;
    }

    @Override
    public Optional<JobPosition> findById(int positionId) {
        return queryOne("SELECT * FROM job_positions WHERE position_id = ?", mapper, positionId);
    }

    @Override
    public List<JobPosition> findAll() {
        return queryList("SELECT * FROM job_positions ORDER BY position_id", mapper);
    }

    @Override
    public List<JobPosition> findByDepartment(int departmentId) {
        return queryList("SELECT * FROM job_positions WHERE department_id = ? ORDER BY title", mapper, departmentId);
    }

    @Override
    public void update(JobPosition p) {
        String sql = "UPDATE job_positions SET title = ?, department_id = ?, description = ? WHERE position_id = ?";
        executeUpdate(sql, p.getTitle(), p.getDepartmentId(), p.getDescription(), p.getPositionId());
    }

    @Override
    public void deleteById(int positionId) {
        executeUpdate("DELETE FROM job_positions WHERE position_id = ?", positionId);
    }
}
