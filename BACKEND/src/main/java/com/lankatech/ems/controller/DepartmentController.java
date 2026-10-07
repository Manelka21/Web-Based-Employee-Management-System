package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateDepartmentRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.Department;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping
    public ResponseEntity<Department> create(@Valid @RequestBody CreateDepartmentRequest request,
                                             @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departmentService.create(request, me.getUserId()));
    }

    // Department list is reference data: any logged-in user may read it
    @GetMapping
    public ResponseEntity<List<Department>> findAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> findById(@PathVariable int id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable int id, @Valid @RequestBody CreateDepartmentRequest request,
                                             @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(departmentService.update(id, request, me.getUserId()));
    }

    // Permanent delete of an empty department (its job positions go with it)
    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        departmentService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Department deleted", true));
    }
}
