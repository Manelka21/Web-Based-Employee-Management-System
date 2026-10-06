package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateHolidayRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.PublicHoliday;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.HolidayService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

// Public holiday calendar. Leave requests don't count these days.
@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    // /api/holidays?year=2026 (defaults to this year)
    @GetMapping
    public ResponseEntity<List<PublicHoliday>> list(@RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(holidayService.findByYear(year != null ? year : LocalDate.now().getYear()));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @PostMapping
    public ResponseEntity<PublicHoliday> add(@Valid @RequestBody CreateHolidayRequest request,
                                             @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(holidayService.add(request, me.getUserId()));
    }

    @PreAuthorize("hasRole('HR_MANAGER')")
    @DeleteMapping("/{date}")
    public ResponseEntity<ApiResponse> delete(@PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                              @AuthenticationPrincipal AppUserPrincipal me) {
        holidayService.delete(date, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Holiday removed", true));
    }
}
