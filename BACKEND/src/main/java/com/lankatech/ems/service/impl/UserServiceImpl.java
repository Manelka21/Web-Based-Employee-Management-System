package com.lankatech.ems.service.impl;

import com.lankatech.ems.dao.EmployeeDao;
import com.lankatech.ems.dao.UserDao;
import com.lankatech.ems.dto.request.CreateUserRequest;
import com.lankatech.ems.dto.request.RegisterRequest;
import com.lankatech.ems.dto.response.UserResponse;
import com.lankatech.ems.enums.EmployeeStatus;
import com.lankatech.ems.enums.Role;
import com.lankatech.ems.exception.DuplicateRecordException;
import com.lankatech.ems.exception.ResourceNotFoundException;
import com.lankatech.ems.model.Employee;
import com.lankatech.ems.model.User;
import com.lankatech.ems.service.ActivityLogService;
import com.lankatech.ems.service.NotificationService;
import com.lankatech.ems.service.UserService;
import com.lankatech.ems.util.EnumUtils;
import com.lankatech.ems.util.ValidationRules;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final EmployeeDao employeeDao;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;

    public UserServiceImpl(UserDao userDao, EmployeeDao employeeDao, PasswordEncoder passwordEncoder,
                           ActivityLogService activityLogService, NotificationService notificationService) {
        this.userDao = userDao;
        this.employeeDao = employeeDao;
        this.passwordEncoder = passwordEncoder;
        this.activityLogService = activityLogService;
        this.notificationService = notificationService;
    }

    /**
     * Public self-registration, open only to people HR already employs:
     *   - the email must belong to an employee record, and the NIC must match it
     *     (so nobody can register with a colleague's email)
     *   - the employee must be active and not linked to another account
     * The account is always EMPLOYEE and is linked to that record straight away.
     * A requested management role is recorded as an access request for the IT Admin.
     */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest r) {
        Role requested = null;
        if (r.getRole() != null && !r.getRole().isBlank()) {
            requested = EnumUtils.parse(Role.class, r.getRole(), "role");
        }

        String email = ValidationRules.normalizeEmail(r.getEmail());
        String genericError = "We couldn't match that email and NIC to an employee record. "
                + "Check both, or ask HR to add or correct your record.";
        Employee employee = employeeDao.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(genericError));
        if (!ValidationRules.normalizeNic(r.getNic()).equalsIgnoreCase(employee.getNic())) {
            throw new IllegalArgumentException(genericError);
        }
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException("Your employee record is inactive. Contact HR.");
        }
        if (userDao.findByEmployeeId(employee.getEmployeeId()).isPresent()) {
            throw new DuplicateRecordException("An account already exists for this employee. Sign in, or use 'Forgot password'.");
        }

        checkPasswordNotEmailName(email, r.getPassword());
        if (userDao.existsByEmail(email)) {
            throw new DuplicateRecordException("Email already registered: " + email);
        }

        User u = new User(email, passwordEncoder.encode(r.getPassword()), employee.getFirstName(), employee.getLastName(), Role.EMPLOYEE);
        u.setPhoneNumber(ValidationRules.normalizePhone(r.getPhoneNumber()) != null
                ? ValidationRules.normalizePhone(r.getPhoneNumber()) : employee.getPhone());
        u.setEmployeeId(employee.getEmployeeId());
        User saved = userDao.save(u);
        activityLogService.log(saved.getUserId(), "REGISTER",
                "Self-registered account " + saved.getEmail() + " linked to employee #" + employee.getEmployeeId());

        if (requested != null && requested != Role.EMPLOYEE) {
            activityLogService.log(saved.getUserId(), "REQUEST_ROLE",
                    "Requested " + requested + " access for " + saved.getEmail());
            notificationService.sendToRole(Role.IT_ADMIN, "ACCESS_REQUEST",
                    saved.getFullName() + " (" + saved.getEmail() + ") requested " + requested
                            + " access. Review it in Users & access.");
        }
        return toResponse(requireUser(saved.getUserId()));
    }

    // IT Admin creates an account with any role.
    @Override
    public UserResponse createUser(CreateUserRequest request, int adminUserId) {
        Role role = Role.EMPLOYEE;
        if (request.getRole() != null && !request.getRole().isBlank()) {
            role = EnumUtils.parse(Role.class, request.getRole(), "role");
        }
        User saved = saveNewUser(request, role);
        // The admin chose this password, so the user must replace it at first sign-in
        userDao.setMustChangePassword(saved.getUserId(), true);
        activityLogService.log(adminUserId, "CREATE_USER",
                "Created user #" + saved.getUserId() + " " + saved.getEmail() + " with role " + role);
        return toResponse(requireUser(saved.getUserId()));
    }

    @Override
    public Optional<User> findById(int userId) {
        return userDao.findById(userId);
    }

    @Override
    public UserResponse getById(int userId) {
        return toResponse(requireUser(userId));
    }

    @Override
    public List<UserResponse> findAll() {
        return userDao.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> findByRole(String role) {
        Role parsed = EnumUtils.parse(Role.class, role, "role");
        return userDao.findByRole(parsed.name()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse changeRole(int userId, String role, int adminUserId) {
        User user = requireUser(userId);
        Role newRole = EnumUtils.parse(Role.class, role, "role");

        if (userId == adminUserId) {
            throw new IllegalArgumentException("You cannot change your own role");
        }

        userDao.updateRole(userId, newRole.name());
        activityLogService.log(adminUserId, "CHANGE_ROLE",
                "Changed role of user #" + userId + " from " + user.getRole() + " to " + newRole);
        notificationService.send(userId, "ROLE_CHANGED", "Your access level was changed to " + newRole + ".");
        return toResponse(requireUser(userId));
    }

    @Override
    public UserResponse linkEmployee(int userId, int employeeId, int adminUserId) {
        requireUser(userId);
        Employee employee = employeeDao.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found: " + employeeId));
        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new IllegalArgumentException("Employee #" + employeeId + " is inactive and cannot be linked to an account");
        }

        // 1:1 rule — an employee record can belong to only one user account
        Optional<User> alreadyLinked = userDao.findByEmployeeId(employeeId);
        if (alreadyLinked.isPresent() && alreadyLinked.get().getUserId() != userId) {
            throw new DuplicateRecordException("Employee #" + employeeId + " is already linked to user "
                    + alreadyLinked.get().getEmail());
        }

        userDao.linkEmployee(userId, employeeId);
        activityLogService.log(adminUserId, "LINK_EMPLOYEE", "Linked user #" + userId + " to employee #" + employeeId);
        return toResponse(requireUser(userId));
    }

    // Admin reset: the user must choose their own password at next sign-in, and any lock is lifted.
    @Override
    @Transactional
    public void resetPassword(int userId, String newPassword, int adminUserId) {
        if (userId == adminUserId) {
            throw new IllegalArgumentException("Change your own password from My profile instead");
        }
        requireUser(userId);
        userDao.updatePassword(userId, passwordEncoder.encode(newPassword));
        userDao.setMustChangePassword(userId, true);
        userDao.clearLoginFailures(userId);
        activityLogService.log(adminUserId, "RESET_PASSWORD", "Reset password of user #" + userId);
        notificationService.send(userId, "PASSWORD_RESET",
                "Your password was reset by an IT administrator. You'll be asked to choose a new one when you sign in.");
    }

    // IT Admin disables/enables an account. Disabled accounts can't sign in and open sessions end.
    @Override
    public UserResponse setActive(int userId, boolean active, int adminUserId) {
        User user = requireUser(userId);
        if (userId == adminUserId) {
            throw new IllegalArgumentException("You cannot disable your own account");
        }
        if (user.isActive() == active) {
            throw new IllegalArgumentException("The account is already " + (active ? "enabled" : "disabled"));
        }
        userDao.setActive(userId, active);
        activityLogService.log(adminUserId, active ? "ENABLE_USER" : "DISABLE_USER",
                (active ? "Enabled" : "Disabled") + " user #" + userId + " " + user.getEmail());
        return toResponse(requireUser(userId));
    }

    @Override
    public UserResponse unlock(int userId, int adminUserId) {
        User user = requireUser(userId);
        if (!user.isLocked() && user.getFailedLoginAttempts() == 0) {
            throw new IllegalArgumentException("The account isn't locked");
        }
        userDao.clearLoginFailures(userId);
        activityLogService.log(adminUserId, "UNLOCK_USER", "Unlocked user #" + userId + " " + user.getEmail());
        return toResponse(requireUser(userId));
    }

    @Override
    @Transactional
    public void changeOwnPassword(int userId, String currentPassword, String newPassword) {
        User user = requireUser(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Your current password is wrong");
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Choose a password different from your current one");
        }
        checkPasswordNotEmailName(user.getEmail(), newPassword);

        userDao.updatePassword(userId, passwordEncoder.encode(newPassword));
        userDao.setMustChangePassword(userId, false);
        activityLogService.log(userId, "CHANGE_PASSWORD", "Changed own password");
    }

    /**
     * "Forgot password": there is no email service to send a reset link, so the request
     * goes to the IT Administrators, who reset it (the user then picks a new one).
     * The caller always gets the same answer, so this can't be used to find out
     * which emails have accounts.
     */
    @Override
    public void requestPasswordReset(String email) {
        Optional<User> user = userDao.findByEmail(ValidationRules.normalizeEmail(email));
        if (user.isEmpty() || !user.get().isActive()) {
            return;
        }
        User u = user.get();
        activityLogService.log(u.getUserId(), "PASSWORD_RESET_REQUEST", "Password reset requested for " + u.getEmail());
        notificationService.sendToRole(Role.IT_ADMIN, "PASSWORD_RESET_REQUEST",
                u.getFullName() + " (" + u.getEmail() + ") forgot their password. Reset it in Users & access.");
    }

    // Manual entity -> DTO conversion (no MapStruct)
    @Override
    public UserResponse toResponse(User u) {
        UserResponse response = new UserResponse(u.getUserId(), u.getEmail(), u.getFullName(), u.getRole().name());
        response.setPhoneNumber(u.getPhoneNumber());
        response.setEmployeeId(u.getEmployeeId());
        response.setCreatedAt(u.getCreatedAt());
        response.setActive(u.isActive());
        response.setLocked(u.isLocked());
        response.setLockedUntil(u.isLocked() ? u.getLockedUntil() : null);
        response.setMustChangePassword(u.isMustChangePassword());
        return response;
    }

    private User saveNewUser(CreateUserRequest r, Role role) {
        String email = ValidationRules.normalizeEmail(r.getEmail());
        if (userDao.existsByEmail(email)) {
            throw new DuplicateRecordException("Email already registered: " + email);
        }
        checkPasswordNotEmailName(email, r.getPassword());

        User u = new User(email, passwordEncoder.encode(r.getPassword()), r.getFirstName().trim(), r.getLastName().trim(), role);
        u.setPhoneNumber(ValidationRules.normalizePhone(r.getPhoneNumber()));
        return userDao.save(u);
    }

    // The password must not simply be the user's email name (e.g. "nadeesha1" for nadeesha@...)
    private void checkPasswordNotEmailName(String email, String password) {
        String localPart = email.substring(0, email.indexOf('@'));
        if (localPart.length() >= 4 && password.toLowerCase().contains(localPart.toLowerCase())) {
            throw new IllegalArgumentException("The password must not contain your email name");
        }
    }

    private User requireUser(int userId) {
        return userDao.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }
}
