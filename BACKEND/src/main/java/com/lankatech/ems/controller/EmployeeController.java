package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.RegisterEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateEmployeeRequest;
import com.lankatech.ems.dto.request.UpdateProfileRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    // HR creates employee records
    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping
    public ResponseEntity<Employee> create(@Valid @RequestBody RegisterEmployeeRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.register(request, me.getUserId()));
    }

    // Self-service: my own employee record
    @GetMapping("/me")
    public ResponseEntity<Employee> myProfile(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(employeeService.getMyProfile(me.getUserId()));
    }

    // Self-service: update my phone/address
    @PutMapping("/me")
    public ResponseEntity<Employee> updateMyProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                    @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(employeeService.updateMyProfile(me.getUserId(), request));
    }

    // HR/Director/Payroll: any record; Supervisor: own team; Employee: own record only
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/{id}")
    public ResponseEntity<Employee> findById(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(employeeService.getForUser(id, me.getUserId()));
    }

    @PreAuthorize("hasAnyRole('HR_MANAGER', 'DEPT_SUPERVISOR', 'COMPANY_DIRECTOR', 'PAYROLL_EXECUTIVE')")
    @GetMapping
    public ResponseEntity<List<Employee>> findAll() {
        return ResponseEntity.ok(employeeService.findAll());
    }

    @PreAuthorize("hasAnyRole('HR_MANAGER', 'DEPT_SUPERVISOR', 'COMPANY_DIRECTOR')")
    @GetMapping("/department/{deptId}")
    public ResponseEntity<List<Employee>> findByDepartment(@PathVariable int deptId,
                                                           @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(employeeService.findByDepartment(deptId, me.getUserId()));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable int id, @Valid @RequestBody UpdateEmployeeRequest request,
                                           @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(employeeService.update(id, request, me.getUserId()));
    }

    // Permanent delete (with the employee's records). To keep history, edit the status to INACTIVE instead.
    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        employeeService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Employee deleted", true));
    }
}
