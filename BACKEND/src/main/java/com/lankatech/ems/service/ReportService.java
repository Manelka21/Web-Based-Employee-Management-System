package com.lankatech.ems.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReportService {

    // ---------- dashboard ----------
    Map<String, Object> dashboardSummary();
    Map<String, Object> departmentSummary(int departmentId);

    // ---------- reports ----------
    List<Map<String, Object>> headcount();
    List<Map<String, Object>> attendance(LocalDate start, LocalDate end);
    List<Map<String, Object>> leave(int year);
    List<Map<String, Object>> payroll(LocalDate start, LocalDate end);
    List<Map<String, Object>> training();
    List<Map<String, Object>> performance();
    List<Map<String, Object>> recruitment();
}
