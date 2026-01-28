package com.badminton.controller;

import com.badminton.dto.request.CreateUserRequest;
import com.badminton.dto.request.UpdateUserRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.UserResponse;
import com.badminton.dto.response.UserDetailResponse;
import com.badminton.dto.response.UserStatisticsResponse;
import com.badminton.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Require ADMIN role for all endpoints
public class UserAdminController {

    private final UserService userService;

    /**
     * Get all users with pagination and sorting
     * GET /admin/users?page=0&size=10&sortBy=createdAt&sortDirection=DESC
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("📋 Admin fetching all users - page: {}, size: {}, sortBy: {}, sortDirection: {}",
                page, size, sortBy, sortDirection);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDetailResponse> users = userService.getAllUsersAdmin(pageable);

        return ResponseEntity.ok(ApiResponse.<Page<UserDetailResponse>>builder()
                .success(true)
                .message("Lấy danh sách người dùng thành công")
                .data(users)
                .build());
    }

    /**
     * Search users with filters
     * GET /admin/users/search?keyword=john&role=USER&active=true&page=0&size=10
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserDetailResponse>>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.info("🔍 Admin searching users - keyword: {}, role: {}, active: {}", keyword, role, active);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<UserDetailResponse> users = userService.searchUsers(keyword, role, active, pageable);

        return ResponseEntity.ok(ApiResponse.<Page<UserDetailResponse>>builder()
                .success(true)
                .message("Tìm kiếm người dùng thành công")
                .data(users)
                .build());
    }

    /**
     * Get user detail by ID
     * GET /admin/users/1
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable Long id) {
        log.info("👤 Admin fetching user detail for ID: {}", id);

        UserDetailResponse user = userService.getUserDetailById(id);

        return ResponseEntity.ok(ApiResponse.<UserDetailResponse>builder()
                .success(true)
                .message("Lấy thông tin người dùng thành công")
                .data(user)
                .build());
    }

    /**
     * Create new user
     * POST /admin/users
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("📝 Admin creating new user: {}", request.getEmail());

        UserResponse user = userService.createUser(request);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Tạo người dùng thành công")
                .data(user)
                .build());
    }

    /**
     * Update user
     * PUT /admin/users/1
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        log.info("📝 Admin updating user ID: {}", id);

        UserResponse user = userService.updateUser(id, request);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .success(true)
                .message("Cập nhật người dùng thành công")
                .data(user)
                .build());
    }

    /**
     * Delete user
     * DELETE /admin/users/1
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        log.info("🗑️ Admin deleting user ID: {}", id);

        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa người dùng thành công")
                .build());
    }

    /**
     * Toggle user active status
     * PATCH /admin/users/1/status?active=false
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {

        log.info("🔄 Admin toggling user status - ID: {}, active: {}", id, active);

        userService.toggleUserStatus(id, active);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật trạng thái người dùng thành công")
                .build());
    }

    /**
     * Update user role
     * PATCH /admin/users/1/role?role=ADMIN
     */
    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long id,
            @RequestParam String role) {

        log.info("🔄 Admin updating user role - ID: {}, role: {}", id, role);

        userService.updateUserRole(id, role);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Cập nhật vai trò người dùng thành công")
                .build());
    }

    /**
     * Get user statistics
     * GET /admin/users/statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<UserStatisticsResponse>> getUserStatistics() {
        log.info("📊 Admin fetching user statistics");

        UserStatisticsResponse stats = userService.getUserStatistics();

        return ResponseEntity.ok(ApiResponse.<UserStatisticsResponse>builder()
                .success(true)
                .message("Lấy thống kê người dùng thành công")
                .data(stats)
                .build());
    }

    /**
     * Reset user password (admin only)
     * POST /admin/users/1/reset-password
     */
    @PostMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetUserPassword(
            @PathVariable Long id,
            @RequestBody String newPassword) {

        log.info("🔐 Admin resetting password for user ID: {}", id);

        userService.resetUserPassword(id, newPassword);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Đặt lại mật khẩu thành công")
                .build());
    }
}
