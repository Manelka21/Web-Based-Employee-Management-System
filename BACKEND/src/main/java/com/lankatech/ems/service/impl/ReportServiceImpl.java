package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.*;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.model.LeaveRequest;
import com.lankatech.ems.service.HolidayService;
import com.lankatech.ems.service.ReportService;
import com.lankatech.ems.util.LeaveBalanceCalculator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

// Read-only aggregation for the dashboard and reports. It owns no data.
@Service
public class ReportServiceImpl implements ReportService {

    private final EmployeeDao employeeDao;
    private final DepartmentDao departmentDao;
    private final VacancyDao vacancyDao;
    private final CandidateApplicationDao applicationDao;
    private final LeaveRequestDao leaveRequestDao;
    private final AttendanceDao attendanceDao;
    private final PayrollDao payrollDao;
    private final ReportDao reportDao;
    private final HolidayService holidayService;

    public ReportServiceImpl(EmployeeDao employeeDao, DepartmentDao departmentDao, VacancyDao vacancyDao,
                             CandidateApplicationDao applicationDao, LeaveRequestDao leaveRequestDao,
                             AttendanceDao attendanceDao, PayrollDao payrollDao, ReportDao reportDao,
                             HolidayService holidayService) {
        this.holidayService = holidayService;
        this.employeeDao = employeeDao;
        this.departmentDao = departmentDao;
        this.vacancyDao = vacancyDao;
        this.applicationDao = applicationDao;
        this.leaveRequestDao = leaveRequestDao;
        this.attendanceDao = attendanceDao;
        this.payrollDao = payrollDao;
        this.reportDao = reportDao;
    }

    @Override
    public Map<String, Object> dashboardSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalEmployees", employeeDao.countAll());
        data.put("activeEmployees", employeeDao.countByStatus("ACTIVE"));
        data.put("employeesOnProbation", employeeDao.countByStatus("PROBATION"));
        data.put("employeesOnLeave", employeeDao.countByStatus("ON_LEAVE"));
        data.put("departments", departmentDao.findAll().size());
        data.put("activeVacancies", vacancyDao.countByStatus("OPEN"));
        data.put("newApplications", applicationDao.countByStatus("APPLIED"));
        data.put("pendingLeaveRequests", leaveRequestDao.countByStatus("PENDING"));
        data.put("todayAttendance", attendanceDao.countByDate(LocalDate.now()));
        data.put("draftPayrolls", payrollDao.countByStatus("DRAFT"));
        return data;
    }

    @Override
    public Map<String, Object> departmentSummary(int departmentId) {
        Department department = departmentDao.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + departmentId));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("departmentId", department.getDepartmentId());
        data.put("departmentName", department.getName());
        data.put("status", department.getStatus());
        data.put("employeeCount", employeeDao.countByDepartment(departmentId));
        data.put("pendingLeaves", leaveRequestDao.countPendingByDepartment(departmentId));
        return data;
    }

    @Override
    public List<Map<String, Object>> headcount() {
        return reportDao.headcountByDepartment();
    }

    @Override
    public List<Map<String, Object>> attendance(LocalDate start, LocalDate end) {
        checkRange(start, end);
        return reportDao.attendanceSummary(start, end);
    }

    @Override
    public List<Map<String, Object>> leave(int year) {
        // Done in Java rather than SQL so totals are working days (no weekends or public
        // holidays) and a request over New Year only counts the days inside this year.
        Set<LocalDate> holidays = holidayService.datesBetween(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        Map<String, Map<String, Object>> groups = new TreeMap<>();
        for (LeaveRequest l : leaveRequestDao.findOverlappingYear(year)) {
            String key = l.getLeaveType().name() + "|" + l.getLeaveStatus().name();
            Map<String, Object> row = groups.computeIfAbsent(key, k -> {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("leaveType", l.getLeaveType().name());
                r.put("status", l.getLeaveStatus().name());
                r.put("requests", 0L);
                r.put("totalDays", 0L);
                return r;
            });
            row.put("requests", (Long) row.get("requests") + 1);
            row.put("totalDays", (Long) row.get("totalDays")
                    + LeaveBalanceCalculator.workingDaysInYear(l.getStartDate(), l.getEndDate(), year, holidays));
        }
        return new ArrayList<>(groups.values());
    }

    @Override
    public List<Map<String, Object>> payroll(LocalDate start, LocalDate end) {
        checkRange(start, end);
        return reportDao.payrollSummary(start, end);
    }

    @Override
    public List<Map<String, Object>> training() {
        return reportDao.trainingSummary();
    }

    @Override
    public List<Map<String, Object>> performance() {
        return reportDao.performanceSummary();
    }

    @Override
    public List<Map<String, Object>> recruitment() {
        return reportDao.recruitmentSummary();
    }

    private void checkRange(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
    }
}
