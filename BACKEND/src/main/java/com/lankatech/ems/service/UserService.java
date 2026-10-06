package com.lankatech.ems.service;

import com.lankatech.ems.dto.request.CreateUserRequest;
import com.lankatech.ems.dto.request.RegisterRequest;
import com.lankatech.ems.dto.response.UserResponse;
import com.lankatech.ems.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    UserResponse register(RegisterRequest request);
    UserResponse createUser(CreateUserRequest request, int adminUserId);
    Optional<User> findById(int userId);
    UserResponse getById(int userId);
    List<UserResponse> findAll();
    List<UserResponse> findByRole(String role);
    UserResponse changeRole(int userId, String role, int adminUserId);
    UserResponse linkEmployee(int userId, int employeeId, int adminUserId);
    void resetPassword(int userId, String newPassword, int adminUserId);
    UserResponse setActive(int userId, boolean active, int adminUserId);
    UserResponse unlock(int userId, int adminUserId);
    void changeOwnPassword(int userId, String currentPassword, String newPassword);
    void requestPasswordReset(String email);
    UserResponse toResponse(User user);
}
