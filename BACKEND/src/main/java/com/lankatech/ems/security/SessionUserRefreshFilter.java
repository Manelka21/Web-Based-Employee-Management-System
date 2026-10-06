package com.lankatech.ems.security;

import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * A session keeps the user's role from the moment they logged in. Without this filter,
 * an IT Admin demoting a user (or HR deactivating their employee record) would only take
 * effect after that user logged out.
 *
 * On every request this filter re-reads the user from the database:
 *   - user gone or deactivated  -> the session is ended (the request becomes anonymous -> 401)
 *   - role or employee link changed -> the session is updated with the fresh user
 *
 * Registered in SecurityConfig (not a @Component, so it isn't added twice as a servlet filter).
 */
public class SessionUserRefreshFilter extends OncePerRequestFilter {

    private final UserDao userDao;
    private final AppUserDetailsService userDetailsService;
    private final SecurityContextRepository repository = new HttpSessionSecurityContextRepository();

    public SessionUserRefreshFilter(UserDao userDao, AppUserDetailsService userDetailsService) {
        this.userDao = userDao;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal current) {
            Optional<User> fresh = userDao.findById(current.getUserId());

            if (fresh.isEmpty() || !userDetailsService.isActive(fresh.get())) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            } else if (fresh.get().isMustChangePassword() && !request.getRequestURI().startsWith("/api/auth/")) {
                // After an admin reset, the temporary password may only be used to choose a new one
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\":403,\"error\":\"PASSWORD_CHANGE_REQUIRED\","
                        + "\"message\":\"Please choose a new password before continuing.\"}");
                return;
            } else if (changed(current.getUser(), fresh.get())) {
                AppUserPrincipal updated = new AppUserPrincipal(fresh.get());
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(updated, null, updated.getAuthorities()));
                SecurityContextHolder.setContext(context);
                if (request.getSession(false) != null) {
                    repository.saveContext(context, request, response);
                }
            }
        }

        chain.doFilter(request, response);
    }

    private boolean changed(User before, User after) {
        return before.getRole() != after.getRole()
                || !Objects.equals(before.getEmployeeId(), after.getEmployeeId())
                || !Objects.equals(before.getEmail(), after.getEmail());
    }
}
