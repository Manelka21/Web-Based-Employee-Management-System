package com.lankatech.ems.dao.impl;

import com.lankatech.ems.dao.AbstractJdbcDao;
import com.lankatech.ems.dao.ReportDao;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Aggregate (GROUP BY) queries. Results don't match one entity,
// so every method uses queryForMaps(...) instead of a RowMapper.
@Repository
public class ReportDaoImpl extends AbstractJdbcDao<Map<String, Object>, Integer> implements ReportDao {

    public ReportDaoImpl(DataSource dataSource) {
        super(dataSource);
    }

    @Override
    public List<Map<String, Object>> headcountByDepartment() {
        String sql =
            "SELECT d.department_id AS departmentId, d.name AS departmentName, " +
            "       COUNT(e.employee_id) AS totalEmployees, " +
            "       SUM(CASE WHEN e.status = 'ACTIVE' THEN 1 ELSE 0 END) AS activeEmployees, " +
            "       SUM(CASE WHEN e.status = 'PROBATION' THEN 1 ELSE 0 END) AS probationEmployees " +
            "FROM departments d " +
            "LEFT JOIN employees e ON e.department_id = d.department_id " +
            "GROUP BY d.department_id, d.name " +
            "ORDER BY d.name";
        return queryForMaps(sql);
    }

    @Override
    public List<Map<String, Object>> attendanceSummary(LocalDate start, LocalDate end) {
        String sql =
            "SELECT e.employee_id AS employeeId, CONCAT(e.first_name, ' ', e.last_name) AS employeeName, " +
            "       e.department_id AS departmentId, " +
            "       SUM(CASE WHEN a.status = 'PRESENT'  THEN 1 ELSE 0 END) AS presentDays, " +
            "       SUM(CASE WHEN a.status = 'LATE'     THEN 1 ELSE 0 END) AS lateDays, " +
            "       SUM(CASE WHEN a.status = 'HALF_DAY' THEN 1 ELSE 0 END) AS halfDays, " +
            "       SUM(CASE WHEN a.status = 'ABSENT'   THEN 1 ELSE 0 END) AS absentDays " +
            "FROM attendance_records a " +
            "JOIN employees e ON a.employee_id = e.employee_id " +
            "WHERE a.date BETWEEN ? AND ? " +
            "GROUP BY e.employee_id, e.first_name, e.last_name, e.department_id " +
            "ORDER BY e.employee_id";
        return queryForMaps(sql, start, end);
    }

    @Override
    public List<Map<String, Object>> payrollSummary(LocalDate start, LocalDate end) {
        String sql =
            "SELECT status, COUNT(*) AS records, " +
            "       SUM(base_salary) AS totalBaseSalary, SUM(overtime_amount) AS totalOvertime, " +
            "       SUM(deductions) AS totalDeductions, SUM(net_pay) AS totalNetPay " +
            "FROM payroll_records " +
            "WHERE pay_period_start >= ? AND pay_period_end <= ? " +
            "GROUP BY status " +
            "ORDER BY status";
        return queryForMaps(sql, start, end);
    }

    @Override
    public List<Map<String, Object>> trainingSummary() {
        String sql =
            "SELECT p.program_id AS programId, p.title, p.status, p.capacity, " +
            "       COUNT(en.enrollment_id) AS totalEnrollments, " +
            "       SUM(CASE WHEN en.completion_status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed, " +
            "       SUM(CASE WHEN en.completion_status = 'DROPPED' THEN 1 ELSE 0 END) AS dropped " +
            "FROM training_programs p " +
            "LEFT JOIN training_enrollments en ON en.program_id = p.program_id " +
            "GROUP BY p.program_id, p.title, p.status, p.capacity " +
            "ORDER BY p.program_id";
        return queryForMaps(sql);
    }

    @Override
    public List<Map<String, Object>> performanceSummary() {
        String sql =
            "SELECT e.employee_id AS employeeId, CONCAT(e.first_name, ' ', e.last_name) AS employeeName, " +
            "       e.department_id AS departmentId, COUNT(pr.performance_id) AS reviews, " +
            "       ROUND(AVG(pr.rating), 2) AS averageRating, MAX(pr.review_date) AS lastReviewDate " +
            "FROM performance_records pr " +
            "JOIN employees e ON pr.employee_id = e.employee_id " +
            "GROUP BY e.employee_id, e.first_name, e.last_name, e.department_id " +
            "ORDER BY averageRating DESC";
        return queryForMaps(sql);
    }

    @Override
    public List<Map<String, Object>> recruitmentSummary() {
        String sql =
            "SELECT v.vacancy_id AS vacancyId, v.title, v.status, " +
            "       COUNT(a.application_id) AS applications, " +
            "       SUM(CASE WHEN a.status = 'SHORTLISTED' THEN 1 ELSE 0 END) AS shortlisted, " +
            "       SUM(CASE WHEN a.status = 'HIRED' THEN 1 ELSE 0 END) AS hired " +
            "FROM vacancies v " +
            "LEFT JOIN candidate_applications a ON a.vacancy_id = v.vacancy_id " +
            "GROUP BY v.vacancy_id, v.title, v.status " +
            "ORDER BY v.vacancy_id";
        return queryForMaps(sql);
    }
}
