package com.lankatech.ems.controller;

import com.lankatech.ems.model.ActivityLog;
import com.lankatech.ems.service.ActivityLogService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Audit trail — read-only, IT Administrator only.
@RestController
@RequestMapping("/api/activity-logs")
@PreAuthorize("hasRole('IT_ADMIN')")
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    public ActivityLogController(ActivityLogService activityLogService) {
        this.activityLogService = activityLogService;
    }

    // /api/activity-logs?limit=50
    @GetMapping
    public ResponseEntity<List<ActivityLog>> recent(@RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(activityLogService.findRecent(limit));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ActivityLog>> byUser(@PathVariable int userId) {
        return ResponseEntity.ok(activityLogService.findByUser(userId));
    }

    // e.g. /api/activity-logs/action/APPROVE_LEAVE
    @GetMapping("/action/{action}")
    public ResponseEntity<List<ActivityLog>> byAction(@PathVariable String action) {
        return ResponseEntity.ok(activityLogService.findByAction(action));
    }
}
