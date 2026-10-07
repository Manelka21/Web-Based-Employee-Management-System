package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateApplicationRequest;
import com.lankatech.ems.dto.request.UpdateApplicationRequest;
import com.lankatech.ems.dto.request.UpdateStatusRequest;
import com.lankatech.ems.dto.response.ApplicationStatusResponse;
import com.lankatech.ems.model.CandidateApplication;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.CandidateApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Recruitment & Onboarding — candidate applications (HR Manager only)
@RestController
@RequestMapping("/api/applications")
@PreAuthorize("hasRole('HR_MANAGER')")
public class CandidateApplicationController {

    private final CandidateApplicationService applicationService;

    public CandidateApplicationController(CandidateApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    public ResponseEntity<CandidateApplication> create(@Valid @RequestBody CreateApplicationRequest request,
                                                       @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(applicationService.create(request, me.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<CandidateApplication>> findAll() {
        return ResponseEntity.ok(applicationService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CandidateApplication> findById(@PathVariable int id) {
        return ResponseEntity.ok(applicationService.findById(id));
    }

    @GetMapping("/vacancy/{vacancyId}")
    public ResponseEntity<List<CandidateApplication>> findByVacancy(@PathVariable int vacancyId) {
        return ResponseEntity.ok(applicationService.findByVacancy(vacancyId));
    }

    // Edit candidate details while the application is in progress
    @PutMapping("/{id}")
    public ResponseEntity<CandidateApplication> update(@PathVariable int id, @Valid @RequestBody UpdateApplicationRequest request,
                                                       @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(applicationService.update(id, request, me.getUserId()));
    }

    // { "status": "SHORTLISTED" | "INTERVIEWED" | "HIRED" | "REJECTED" | "WITHDRAWN" }
    // HIRED creates an Employee record (returned as createdEmployee).
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationStatusResponse> updateStatus(@PathVariable int id,
                                                                  @Valid @RequestBody UpdateStatusRequest request,
                                                                  @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(applicationService.updateStatus(id, request.getStatus(), me.getUserId()));
    }

    // Withdraw (status -> WITHDRAWN; the record is kept)
    @DeleteMapping("/{id}")
    public ResponseEntity<CandidateApplication> withdraw(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(applicationService.withdraw(id, me.getUserId()));
    }
}
