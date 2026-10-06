package com.lankatech.ems.controller;

import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.model.Notification;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

// Every logged-in user reads their own in-app notifications.
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<Notification>> myNotifications(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(notificationService.findMine(me.getUserId()));
    }

    @GetMapping("/unread")
    public ResponseEntity<List<Notification>> unread(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(notificationService.findUnread(me.getUserId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Integer>> unreadCount(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(Map.of("unread", notificationService.countUnread(me.getUserId())));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse> markRead(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        notificationService.markRead(id, me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Notification marked as read", true));
    }

    // ---------- email outbox (IT Admin): what was emailed, and whether it went out ----------

    @PreAuthorize("hasRole('IT_ADMIN')")
    @GetMapping("/outbox")
    public ResponseEntity<List<Notification>> outbox(@RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(notificationService.outbox(limit));
    }

    @PreAuthorize("hasRole('IT_ADMIN')")
    @PatchMapping("/outbox/{id}/retry")
    public ResponseEntity<Notification> retry(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(notificationService.retry(id, me.getUserId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse> markAllRead(@AuthenticationPrincipal AppUserPrincipal me) {
        notificationService.markAllRead(me.getUserId());
        return ResponseEntity.ok(new ApiResponse("All notifications marked as read", true));
    }
}
