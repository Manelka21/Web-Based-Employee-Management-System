package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.LogAttendanceRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.AttendanceRecord;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // Supervisor (own team) or HR logs daily attendance for an employee
    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @PostMapping
    public ResponseEntity<AttendanceRecord> log(@Valid @RequestBody LogAttendanceRequest request,
                                                @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.log(request, me.getUserId()));
    }

    // Employee self-service
    @PostMapping("/check-in")
    public ResponseEntity<AttendanceRecord> checkIn(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.checkIn(me.getUserId()));
    }

    @PostMapping("/check-out")
    public ResponseEntity<AttendanceRecord> checkOut(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(attendanceService.checkOut(me.getUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<AttendanceRecord>> myAttendance(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(attendanceService.findMine(me.getUserId()));
    }

    // EMPLOYEE (own), SUPERVISOR (team), HR / Payroll (read-only) / Director
    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<AttendanceRecord>> findByEmployee(@PathVariable int empId,
                                                                 @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(attendanceService.findByEmployee(empId, me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER', 'PAYROLL_EXECUTIVE', 'COMPANY_DIRECTOR')")
    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<AttendanceRecord>> findByDepartment(@PathVariable int deptId,
                                                                   @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(attendanceService.findByDepartment(deptId, me.getUserId()));
    }

    // Correct an entry — overrideReason is required
    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceRecord> correct(@PathVariable int id, @Valid @RequestBody LogAttendanceRequest request,
                                                    @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(attendanceService.correct(id, request, me.getUserId()));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        attendanceService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Attendance record removed", true));
    }
}
