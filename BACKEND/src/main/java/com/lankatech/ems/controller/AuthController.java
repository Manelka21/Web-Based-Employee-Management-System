package com.lankatech.ems.controller;

import com.lankatech.ems.dto.request.ChangePasswordRequest;
import com.lankatech.ems.dto.request.ForgotPasswordRequest;
import com.lankatech.ems.dto.request.LoginRequest;
import com.lankatech.ems.dto.request.RegisterRequest;
import com.lankatech.ems.dto.response.ApiResponse;
import com.lankatech.ems.dto.response.UserResponse;
import com.lankatech.ems.security.AppUserPrincipal;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final ActivityLogService activityLogService;

    // Stores the logged-in SecurityContext in the HTTP session (-> JSESSIONID cookie)
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(UserService userService, AuthenticationManager authenticationManager,
                          ActivityLogService activityLogService) {
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.activityLogService = activityLogService;
    }

    // Public self-registration for existing employees (email + NIC must match their record).
    // The account always gets the EMPLOYEE role; other roles are granted by the IT Admin.
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(request));
    }

    // CSRF token for browser clients: send it back in the X-CSRF-TOKEN header on POST/PUT/PATCH/DELETE
    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> csrf(CsrfToken token) {
        return ResponseEntity.ok(Map.of("headerName", token.getHeaderName(), "token", token.getToken()));
    }

    // Signed-in user changes their own password (also clears "must change password")
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse> changePassword(@Valid @RequestBody ChangePasswordRequest body,
                                                      @AuthenticationPrincipal AppUserPrincipal me) {
        userService.changeOwnPassword(me.getUserId(), body.getCurrentPassword(), body.getNewPassword());
        return ResponseEntity.ok(new ApiResponse("Password changed", true));
    }

    // Public. Always answers the same way, whether or not the email has an account.
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest body) {
        userService.requestPasswordReset(body.getEmail());
        return ResponseEntity.ok(new ApiResponse(
                "If that email has an account, the IT administrators have been asked to reset its password.", true));
    }

    // Session login with JSON { "email": ..., "password": ... }.
    // (Postman users can also simply send Basic Auth on every request.)
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest body,
                                              HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(body.getEmail().trim(), body.getPassword()));

        // Protect against session fixation: give an existing session a new id
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();
        activityLogService.log(principal.getUserId(), "LOGIN", "Logged in as " + principal.getUsername());
        return ResponseEntity.ok(userService.toResponse(principal.getUser()));
    }

    // "Who am I?" — returns the logged-in user (fresh from the database).
    @GetMapping("/me")
    public ResponseEntity<UserResponse> whoAmI(@AuthenticationPrincipal AppUserPrincipal me) {
        return ResponseEntity.ok(userService.getById(me.getUserId()));
    }

    // Explicit logout: invalidates the JSESSIONID.
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(new ApiResponse("Logged out", true));
    }
}
