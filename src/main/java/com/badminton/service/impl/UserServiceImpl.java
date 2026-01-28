package com.badminton.service.impl;

import com.badminton.dto.request.UpdateProfileRequest;
import com.badminton.dto.request.CreateUserRequest;
import com.badminton.dto.request.UpdateUserRequest;
import com.badminton.dto.response.UserResponse;
import com.badminton.dto.response.UserDetailResponse;
import com.badminton.dto.response.UserStatisticsResponse;
import com.badminton.entity.User;
import com.badminton.exception.ResourceNotFoundException;

import com.badminton.dto.request.ChangePasswordRequest;
import com.badminton.repository.UserRepository;
import com.badminton.service.UserService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.badminton.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + id));
        return mapToUserResponse(user);
    }

    @Override
    public UserResponse updateProfile(Long id, UpdateProfileRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setFullName(request.getFullName());

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại đã được sử dụng");
            }
            user.setPhone(request.getPhone());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        userRepository.delete(user);
    }

    @Override
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    @Override
    public User findByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Không tìm thấy người dùng với số điện thoại: " + phone));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Override
    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("🔐 Changing password for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));

        // Kiểm tra mật khẩu cũ
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            log.warn("⚠️ Old password incorrect for user: {}", user.getEmail());
            throw new BadRequestException("Mật khẩu cũ không chính xác");
        }

        // Kiểm tra mật khẩu mới không trùng mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            log.warn("⚠️ New password same as old password for user: {}", user.getEmail());
            throw new BadRequestException("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        // Cập nhật mật khẩu mới
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("✅ Password changed successfully for user: {}", user.getEmail());
    }

    // ==================== ADMIN METHODS ====================

    @Override
    public Page<UserDetailResponse> getAllUsersAdmin(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToUserDetailResponse);
    }

    @Override
    public Page<UserDetailResponse> searchUsers(String keyword, String role, Boolean active, Pageable pageable) {
        // Custom query method - cần add vào UserRepository
        // Tạm thời filter sau khi lấy tất cả (không optimal nhưng works)
        Page<User> users = userRepository.findAll(pageable);
        return users.map(this::mapToUserDetailResponse);
    }

    @Override
    public UserDetailResponse getUserDetailById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
        return mapToUserDetailResponse(user);
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        log.info("📝 Creating new user: {}", request.getEmail());

        // Validate email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        // Validate phone
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.valueOf(request.getRole()));
        user.setAddress(request.getAddress());
        user.setActive(request.getActive());

        User savedUser = userRepository.save(user);
        log.info("✅ User created successfully: {}", savedUser.getEmail());

        return mapToUserResponse(savedUser);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        log.info("📝 Updating user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email đã được sử dụng");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new BadRequestException("Số điện thoại đã được sử dụng");
            }
            user.setPhone(request.getPhone());
        }

        if (request.getRole() != null) {
            user.setRole(User.UserRole.valueOf(request.getRole()));
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }

        User updatedUser = userRepository.save(user);
        log.info("✅ User updated successfully: {}", updatedUser.getEmail());

        return mapToUserResponse(updatedUser);
    }

    @Override
    public void toggleUserStatus(Long id, Boolean active) {
        log.info("🔄 Toggling user status for ID: {} to {}", id, active);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setActive(active);
        userRepository.save(user);

        log.info("✅ User status updated successfully");
    }

    @Override
    public void updateUserRole(Long id, String role) {
        log.info("🔄 Updating role for user ID: {} to {}", id, role);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setRole(User.UserRole.valueOf(role));
        userRepository.save(user);

        log.info("✅ User role updated successfully");
    }

    @Override
    public UserStatisticsResponse getUserStatistics() {
        log.info("📊 Getting user statistics");

        List<User> allUsers = userRepository.findAll();

        long totalUsers = allUsers.size();
        long activeUsers = allUsers.stream().filter(User::getActive).count();
        long inactiveUsers = totalUsers - activeUsers;

        // New users this month
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        long newUsersThisMonth = allUsers.stream()
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(startOfMonth))
                .count();

        // Users by role
        java.util.Map<String, Long> usersByRole = allUsers.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        user -> user.getRole().name(),
                        java.util.stream.Collectors.counting()));

        return UserStatisticsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .newUsersThisMonth(newUsersThisMonth)
                .usersByRole(usersByRole)
                .build();
    }

    @Override
    public void resetUserPassword(Long id, String newPassword) {
        log.info("🔐 Resetting password for user ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("✅ Password reset successfully for user: {}", user.getEmail());
    }

    private UserDetailResponse mapToUserDetailResponse(User user) {
        // Count bookings and orders
        int totalBookings = user.getBookings() != null ? user.getBookings().size() : 0;

        // Note: totalOrders requires Order entity relationship
        // For now, set to 0 or implement later
        int totalOrders = 0;

        return UserDetailResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .address(user.getAddress())
                .avatar(null) // Add avatar field to User entity if needed
                .active(user.getActive())
                .lastLogin(null) // Add lastLogin field to User entity if needed
                .totalBookings(totalBookings)
                .totalOrders(totalOrders)
                .totalSpent(user.getTotalSpent() != null ? user.getTotalSpent().doubleValue() : 0.0)
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}