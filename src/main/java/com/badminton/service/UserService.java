package com.badminton.service;

import com.badminton.dto.request.UpdateProfileRequest;
import com.badminton.dto.request.CreateUserRequest;
import com.badminton.dto.request.UpdateUserRequest;
import com.badminton.dto.response.UserResponse;
import com.badminton.dto.response.UserDetailResponse;
import com.badminton.dto.response.UserStatisticsResponse;
import com.badminton.dto.request.ChangePasswordRequest;
import com.badminton.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {
    UserResponse getUserById(Long id);

    UserResponse updateProfile(Long id, UpdateProfileRequest request);

    List<UserResponse> getAllUsers();

    void deleteUser(Long id);

    User findByEmail(String email);

    User findByPhone(String phone);

    void changePassword(Long userId, ChangePasswordRequest request);

    // ==================== ADMIN METHODS ====================

    // Get all users with pagination and sorting
    Page<UserDetailResponse> getAllUsersAdmin(Pageable pageable);

    // Search users with filters
    Page<UserDetailResponse> searchUsers(String keyword, String role, Boolean active, Pageable pageable);

    // Get user detail by ID
    UserDetailResponse getUserDetailById(Long id);

    // Create user (admin only)
    UserResponse createUser(CreateUserRequest request);

    // Update user (admin only)
    UserResponse updateUser(Long id, UpdateUserRequest request);

    // Toggle user active status
    void toggleUserStatus(Long id, Boolean active);

    // Update user role
    void updateUserRole(Long id, String role);

    // Get user statistics
    UserStatisticsResponse getUserStatistics();

    // Reset user password (admin only)
    void resetUserPassword(Long id, String newPassword);
}
