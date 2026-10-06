package com.lankatech.ems.controller;

import com.lankatech.ems.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Read-only aggregation endpoints — no data of their own.
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('COMPANY_DIRECTOR', 'HR_MANAGER')")
public class DashboardController {

    private final ReportService reportService;

    public DashboardController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(reportService.dashboardSummary());
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<Map<String, Object>> departmentSummary(@PathVariable int deptId) {
        return ResponseEntity.ok(reportService.departmentSummary(deptId));
    }
}
