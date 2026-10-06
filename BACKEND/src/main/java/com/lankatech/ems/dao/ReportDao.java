package com.lankatech.ems.dao;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Read-only aggregate queries used by the dashboard and report endpoints.
// Each row is returned as a column-name -> value map.
public interface ReportDao {

    List<Map<String, Object>> headcountByDepartment();
    List<Map<String, Object>> attendanceSummary(LocalDate start, LocalDate end);
    List<Map<String, Object>> payrollSummary(LocalDate start, LocalDate end);
    List<Map<String, Object>> trainingSummary();
    List<Map<String, Object>> performanceSummary();
    List<Map<String, Object>> recruitmentSummary();
}
