package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.CreateUserRequest;
import com.lankatech.ems.dto.request.ResetPasswordRequest;
import com.lankatech.ems.dto.request.UpdateRoleRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.dto.response.UserResponse;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// IT/System Administrator: user accounts and roles.
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('IT_ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll() {
        return ResponseEntity.ok(userService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable int id) {
        return ResponseEntity.ok(userService.getById(id));
    }

    @GetMapping("/role/{role}")
    public ResponseEntity<List<UserResponse>> findByRole(@PathVariable String role) {
        return ResponseEntity.ok(userService.findByRole(role));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request,
                                               @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request, me.getUserId()));
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(@PathVariable int id, @Valid @RequestBody UpdateRoleRequest request,
                                                   @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.changeRole(id, request.getRole(), me.getUserId()));
    }

    @PatchMapping("/{id}/link-employee/{employeeId}")
    public ResponseEntity<UserResponse> linkEmployee(@PathVariable int id, @PathVariable int employeeId,
                                                     @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.linkEmployee(id, employeeId, me.getUserId()));
    }

    @PatchMapping("/{id}/disable")
    public ResponseEntity<UserResponse> disable(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.setActive(id, false, me.getUserId()));
    }

    @PatchMapping("/{id}/enable")
    public ResponseEntity<UserResponse> enable(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.setActive(id, true, me.getUserId()));
    }

    // Lift a lock caused by too many wrong passwords
    @PatchMapping("/{id}/unlock")
    public ResponseEntity<UserResponse> unlock(@PathVariable int id, @AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.unlock(id, me.getUserId()));
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<ApiResponse> resetPassword(@PathVariable int id, @Valid @RequestBody ResetPasswordRequest request,
                                                     @AuthenticationPrincipal AppUserPrincipal me) {
        userService.resetPassword(id, request.getNewPassword(), me.getUserId());
        return ResponseEntity.ok(new ApiResponse("Password reset for user #" + id, true));
    }
}
