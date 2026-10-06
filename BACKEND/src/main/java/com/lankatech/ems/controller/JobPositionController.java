package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateJobPositionRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.JobPosition;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.JobPositionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/positions")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    public JobPositionController(JobPositionService jobPositionService) {
        this.jobPositionService = jobPositionService;
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping
    public ResponseEntity<JobPosition> create(@Valid @RequestBody CreateJobPositionRequest request,
                                              @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(jobPositionService.create(request, me.getUserId()));
    }

    @GetMapping
    public ResponseEntity<List<JobPosition>> findAll() {
        return ResponseEntity.ok(jobPositionService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobPosition> findById(@PathVariable int id) {
        return ResponseEntity.ok(jobPositionService.findById(id));
    }

    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<JobPosition>> findByDepartment(@PathVariable int deptId) {
        return ResponseEntity.ok(jobPositionService.findByDepartment(deptId));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<JobPosition> update(@PathVariable int id, @Valid @RequestBody CreateJobPositionRequest request,
                                              @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(jobPositionService.update(id, request, me.getUserId()));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        jobPositionService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Job position deleted", true));
    }
}
