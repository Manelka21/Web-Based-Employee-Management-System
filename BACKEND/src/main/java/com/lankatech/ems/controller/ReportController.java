package com.lankatech.ems.controller;

import com.lankatech.ems.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

// Company-wide reports. Date parameters are optional and default to the current month.
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER')")
    @GetMapping("/headcount")
    public ResponseEntity<List<Map<String, Object>>> headcount() {
        return ResponseEntity.ok(reportService.headcount());
    }

    // /api/reports/attendance?start=2026-09-01&end=2026-09-30
    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER', 'PAYROLL_EXECUTIVE')")
    @GetMapping("/attendance")
    public ResponseEntity<List<Map<String, Object>>> attendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportService.attendance(startOrDefault(start), endOrDefault(end)));
    }

    // /api/reports/leave?year=2026
    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER', 'PAYROLL_EXECUTIVE')")
    @GetMapping("/leave")
    public ResponseEntity<List<Map<String, Object>>> leave(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(reportService.leave(year != null ? year : LocalDate.now().getYear()));
    }

    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'PAYROLL_EXECUTIVE')")
    @GetMapping("/payroll")
    public ResponseEntity<List<Map<String, Object>>> payroll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(reportService.payroll(startOrDefault(start), endOrDefault(end)));
    }

    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER')")
    @GetMapping("/training")
    public ResponseEntity<List<Map<String, Object>>> training() {
        return ResponseEntity.ok(reportService.training());
    }

    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER')")
    @GetMapping("/performance")
    public ResponseEntity<List<Map<String, Object>>> performance() {
        return ResponseEntity.ok(reportService.performance());
    }

    @PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER')")
    @GetMapping("/recruitment")
    public ResponseEntity<List<Map<String, Object>>> recruitment() {
        return ResponseEntity.ok(reportService.recruitment());
    }

    private LocalDate startOrDefault(LocalDate start) {
        return start != null ? start : YearMonth.now().atDay(1);
    }

    private LocalDate endOrDefault(LocalDate end) {
        return end != null ? end : YearMonth.now().atEndOfMonth();
    }
}
