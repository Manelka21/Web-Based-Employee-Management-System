package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.ApplyLeaveRequest;
import com.lankatech.ems.dto.request.LeaveDecisionRequest;
import com.lankatech.ems.model.LeaveRequest;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // Any user linked to an employee record can apply (for themselves)
    @PostMapping
    public ResponseEntity<LeaveRequest> apply(@Valid @RequestBody ApplyLeaveRequest request,
                                              @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.apply(request, me.getUserId()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LeaveRequest>> myLeave(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.findMine(me.getUserId()));
    }

    @GetMapping("/balance")
    public ResponseEntity<List<Map<String, Object>>> balance(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.balance(me.getUserId()));
    }

    // HR: all pending; Supervisor: own department's pending requests
    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @GetMapping("/pending")
    public ResponseEntity<List<LeaveRequest>> pending(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.findPending(me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER', 'PAYROLL_EXECUTIVE', 'COMPANY_DIRECTOR')")
    @GetMapping("/employee/{empId}")
    public ResponseEntity<List<LeaveRequest>> findByEmployee(@PathVariable int empId,
                                                             @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.findByEmployee(empId, me.getUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeaveRequest> findById(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.findById(id, me.getUserId()));
    }

    // Optional body: { "comment": "..." }
    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @PatchMapping("/{id}/approve")
    public ResponseEntity<LeaveRequest> approve(@PathVariable int id,
                                                @Valid @RequestBody(required = false) LeaveDecisionRequest body,
                                                @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.approve(id, me.getUserId(), body != null ? body.getComment() : null));
    }

    @PreAuthorize("hasAnyRole('DEPT_SUPERVISOR', 'HR_MANAGER')")
    @PatchMapping("/{id}/reject")
    public ResponseEntity<LeaveRequest> reject(@PathVariable int id,
                                               @Valid @RequestBody(required = false) LeaveDecisionRequest body,
                                               @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.reject(id, me.getUserId(), body != null ? body.getComment() : null));
    }

    // The employee cancels their own request (status -> CANCELLED)
    @DeleteMapping("/{id}")
    public ResponseEntity<LeaveRequest> cancel(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(leaveService.cancel(id, me.getUserId()));
    }
}
