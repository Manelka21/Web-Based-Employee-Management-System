package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.PerformanceDao;
import com.lankatech.ems.dao.impl.rowmapper.PerformanceRowMapper;
import com.lankatech.ems.model.PerformanceRecord;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

@Repository
public class PerformanceDaoImpl extends AbstractJdbcDao<PerformanceRecord, Integer> implements PerformanceDao {

    private final PerformanceRowMapper mapper = new PerformanceRowMapper();

    public PerformanceDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public PerformanceRecord save(PerformanceRecord p) {
        String sql =
            "INSERT INTO performance_records (employee_id, supervisor_id, feedback, rating, review_date) " +
            "VALUES (?, ?, ?, ?, ?)";
        int id = executeInsertReturnId(sql,
            p.getEmployeeId(), p.getSupervisorId(), p.getFeedback(), p.getRating(), p.getReviewDate()
        );
        p.setPerformanceId(id);
        return p;
    }

    @Override
    public Optional<PerformanceRecord> findById(int performanceId) {
        return queryOne("SELECT * FROM performance_records WHERE performance_id = ?", mapper, performanceId);
    }

    @Override
    public List<PerformanceRecord> findByEmployee(int employeeId) {
        return queryList("SELECT * FROM performance_records WHERE employee_id = ? ORDER BY review_date DESC", mapper, employeeId);
    }

    @Override
    public List<PerformanceRecord> findByDepartment(int departmentId) {
        String sql =
            "SELECT p.* FROM performance_records p " +
            "JOIN employees e ON p.employee_id = e.employee_id " +
            "WHERE e.department_id = ? ORDER BY p.review_date DESC";
        return queryList(sql, mapper, departmentId);
    }

    @Override
    public void update(PerformanceRecord p) {
        String sql = "UPDATE performance_records SET feedback = ?, rating = ?, review_date = ? WHERE performance_id = ?";
        executeUpdate(sql, p.getFeedback(), p.getRating(), p.getReviewDate(), p.getPerformanceId());
    }

    @Override
    public void deleteById(int performanceId) {
        executeUpdate("DELETE FROM performance_records WHERE performance_id = ?", performanceId);
    }
}
