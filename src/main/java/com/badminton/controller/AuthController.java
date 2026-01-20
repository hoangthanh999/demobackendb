package com.badminton.controller;

import com.badminton.dto.request.LoginRequest;
import com.badminton.dto.request.RegisterRequest;
import com.badminton.dto.request.UpdateProfileRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.AuthResponse;
import com.badminton.dto.response.UserResponse;
import com.badminton.dto.request.ForgotPasswordRequest;
import com.badminton.dto.request.ResetPasswordRequest;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.AuthService;
import com.badminton.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ THÊM
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j // ✅ THÊM
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng ký thành công"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đăng nhập thành công"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserResponse response = userService.getUserById(user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Lấy thông tin thành công"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

        UserResponse response = userService.updateProfile(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật thông tin thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        log.info("📧 Forgot password request for email: {}", request.getEmail()); // ✅ THÊM

        try {
            authService.requestPasswordReset(request.getEmail());
            return ResponseEntity.ok(ApiResponse.success(null,
                    "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi"));
        } catch (Exception e) {
            log.error("❌ Error in forgot password: ", e); // ✅ THÊM
            // Vẫn trả về success để tránh enumerate user
            return ResponseEntity.ok(ApiResponse.success(null,
                    "Nếu email tồn tại, link đặt lại mật khẩu đã được gửi"));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        log.info("🔐 Reset password request with token"); // ✅ THÊM

        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null, "Đặt lại mật khẩu thành công"));
    }
}
