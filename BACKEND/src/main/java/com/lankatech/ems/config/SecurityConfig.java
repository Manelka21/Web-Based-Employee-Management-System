package com.lankatech.ems.config;

import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.security.AppUserDetailsService;
import com.lankatech.ems.security.SessionUserRefreshFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;
import java.time.LocalDateTime;

@Configuration
@EnableMethodSecurity     // enables @PreAuthorize on controller methods
public class SecurityConfig {

    // BCrypt hashing for stored passwords
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    // Exposed so AuthController can log users in with email + password (POST /api/auth/login).
    // Spring builds it from our AppUserDetailsService + PasswordEncoder beans.
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, UserDao userDao,
                                           AppUserDetailsService userDetailsService) throws Exception {
        http
            // Re-read the logged-in user on every request so role changes and
            // deactivations take effect immediately (see SessionUserRefreshFilter)
            .addFilterAfter(new SessionUserRefreshFilter(userDao, userDetailsService), BasicAuthenticationFilter.class)

            // CSRF protection for browser sessions. The browser sends the JSESSIONID cookie
            // automatically, so another website could otherwise trigger requests as the user.
            // The frontend fetches the token from GET /api/auth/csrf and sends it in the
            // X-CSRF-TOKEN header. Requests that carry their own credentials (an Authorization
            // header, e.g. Basic auth from Postman) can't be forged this way, so they're exempt,
            // as are the sign-in/out endpoints that run before a session exists.
            .csrf(csrf -> csrf
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                .ignoringRequestMatchers(request -> request.getHeader("Authorization") != null)
                .ignoringRequestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/logout", "/api/auth/forgot-password"))

            // Use the CORS rules from WebConfig
            .cors(Customizer.withDefaults())

            // Session-based auth. Spring creates a JSESSIONID cookie after login.
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

            // Coarse route-level rules. Finer per-endpoint rules are @PreAuthorize
            // annotations on each controller method.
            .authorizeHttpRequests(auth -> auth
                // browser pre-flight requests
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // public endpoints
                .requestMatchers(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/logout",
                    "/api/auth/csrf",
                    "/api/auth/forgot-password"
                ).permitAll()

                // Public holiday calendar: anyone signed in reads it, HR maintains it (see @PreAuthorize)
                .requestMatchers("/api/holidays/**").authenticated()

                // IT Admin only — user account management, activity logs
                .requestMatchers("/api/users/**").hasRole("IT_ADMIN")
                .requestMatchers("/api/activity-logs/**").hasRole("IT_ADMIN")

                // HR Manager only — recruitment
                .requestMatchers("/api/vacancies/**").hasRole("HR_MANAGER")
                .requestMatchers("/api/applications/**").hasRole("HR_MANAGER")

                // Director / HR — dashboard; reports also for Payroll
                .requestMatchers("/api/dashboard/**").hasAnyRole("COMPANY_DIRECTOR", "HR_MANAGER")
                .requestMatchers("/api/reports/**").hasAnyRole("COMPANY_DIRECTOR", "HR_MANAGER", "PAYROLL_EXECUTIVE")

                // everything else: any logged-in user (then @PreAuthorize decides)
                .anyRequest().authenticated()
            )

            // Basic HTTP auth (email + password) is enough for Postman testing.
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, ex) ->
                writeJsonError(response, HttpStatus.UNAUTHORIZED, "Authentication required: invalid or missing credentials")))

            // JSON error bodies instead of Spring's default HTML/blank responses
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, e) ->
                    writeJsonError(response, HttpStatus.UNAUTHORIZED, "Authentication required: please log in"))
                .accessDeniedHandler((request, response, e) -> {
                    if (e instanceof CsrfException) {
                        // The frontend sees error = "CSRF", fetches a fresh token and retries once
                        writeJsonError(response, HttpStatus.FORBIDDEN, "CSRF",
                            "Your security token is missing or expired. Please try again.");
                    } else {
                        writeJsonError(response, HttpStatus.FORBIDDEN, "Access denied: your role cannot use this endpoint");
                    }
                })
            );

        return http.build();
    }

    private static void writeJsonError(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        writeJsonError(response, status, status.getReasonPhrase(), message);
    }

    private static void writeJsonError(HttpServletResponse response, HttpStatus status, String error, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
            "{\"timestamp\":\"" + LocalDateTime.now() + "\"," +
            "\"status\":" + status.value() + "," +
            "\"error\":\"" + error + "\"," +
            "\"message\":\"" + message + "\"}"
        );
    }
}
