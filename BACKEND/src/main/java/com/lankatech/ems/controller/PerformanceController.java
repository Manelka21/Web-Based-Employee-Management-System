package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.RecordPerformanceRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.PerformanceRecord;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.PerformanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    public PerformanceController(PerformanceService performanceService) {
        this.performanceService = performanceService;
    }

    @PreAuthorize("hasRole('DEPT_SUPERVISOR')")
    @PostMapping
    public ResponseEntity<PerformanceRecord> create(@Valid @RequestBody RecordPerformanceRequest request,
                                                    @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(performanceService.create(request, me.getUserId()));
    }

    // An employee reads their own feedback
    @GetMapping("/my")
    public ResponseEntity<List<PerformanceRecord>> myFeedback(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(performanceService.findMine(me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER', 'COMPANY_DIRECTOR')")
    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<PerformanceRecord>> findByEmployee(@PathVariable int empId,
                                                                  @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(performanceService.findByEmployee(empId, me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER', 'COMPANY_DIRECTOR')")
    @GetMapping("/team/{deptId}")
    public ResponseEntity<List<PerformanceRecord>> findByTeam(@PathVariable int deptId,
                                                              @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(performanceService.findByTeam(deptId, me.getUserId()));
    }

    @PreAuthorize("hasRole('DEPT_SUPERVISOR')")
    @PutMapping("/{id}")
    public ResponseEntity<PerformanceRecord> update(@PathVariable int id, @Valid @RequestBody RecordPerformanceRequest request,
                                                    @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(performanceService.update(id, request, me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        performanceService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Performance record removed", true));
    }
}
