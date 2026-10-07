package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateVacancyRequest;
import com.lankatech.ems.dto.request.UpdateStatusRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.Vacancy;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.VacancyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Recruitment & Onboarding — vacancies (HR Manager only)
@RestController
@RequestMapping("/api/vacancies")
@PreAuthorize("hasRole('HR_MANAGER')")
public class VacancyController {

    private final VacancyService vacancyService;

    public VacancyController(VacancyService vacancyService) {
        this.vacancyService = vacancyService;
    }

    @PostMapping
    public ResponseEntity<Vacancy> create(@Valid @RequestBody CreateVacancyRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vacancyService.create(request, me.getUserId()));
    }

    // Optional filter: /api/vacancies?status=OPEN
    @GetMapping
    public ResponseEntity<List<Vacancy>> findAll(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(vacancyService.findAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vacancy> findById(@PathVariable int id) {
        return ResponseEntity.ok(vacancyService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vacancy> update(@PathVariable int id, @Valid @RequestBody CreateVacancyRequest request,
                                          @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(vacancyService.update(id, request, me.getUserId()));
    }

    @PatchMapping("/{id}/close")
    public ResponseEntity<Vacancy> close(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(vacancyService.close(id, me.getUserId()));
    }

    // { "status": "OPEN" | "CLOSED" | "FILLED" }
    @PatchMapping("/{id}/status")
    public ResponseEntity<Vacancy> changeStatus(@PathVariable int id, @Valid @RequestBody UpdateStatusRequest request,
                                                @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(vacancyService.changeStatus(id, request.getStatus(), me.getUserId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> delete(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        vacancyService.delete(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Vacancy deleted", true));
    }
}
