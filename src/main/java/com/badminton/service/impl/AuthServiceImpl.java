package com.badminton.service.impl;

import com.badminton.dto.request.LoginRequest;
import com.badminton.dto.request.RegisterRequest;
import com.badminton.dto.response.AuthResponse;
import com.badminton.dto.response.UserResponse;
import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.repository.UserRepository;
import com.badminton.security.JwtUtil;
import com.badminton.service.AuthService;
import com.badminton.service.EmailService;
import com.badminton.entity.PasswordResetToken;
import com.badminton.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ THÊM
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j // ✅ THÊM
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;
    private final PasswordResetTokenRepository tokenRepository;

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Số điện thoại đã được sử dụng");
        }

        User user = new User();
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.UserRole.USER);
        user.setActive(true);

        User savedUser = userRepository.save(user);
        String token = jwtUtil.generateTokenFromEmail(savedUser.getEmail());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(mapToUserResponse(savedUser))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmailOrPhone(),
                        request.getPassword()));

        String token = jwtUtil.generateToken(authentication);

        User user = userRepository.findByEmail(request.getEmailOrPhone())
                .orElseGet(() -> userRepository.findByPhone(request.getEmailOrPhone())
                        .orElseThrow(() -> new BadRequestException("Thông tin đăng nhập không chính xác")));

        if (!user.getActive()) {
            throw new BadRequestException("Tài khoản đã bị khóa");
        }

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(mapToUserResponse(user))
                .build();
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
    public void requestPasswordReset(String email) {
        log.info("🔍 Looking for user with email: {}", email); // ✅ THÊM

        // Tìm user - không throw exception để tránh enumerate user
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            log.warn("⚠️ User not found for email: {}", email); // ✅ THÊM
            // Không báo lỗi để tránh tiết lộ email có tồn tại hay không
            return;
        }

        log.info("✅ User found: {}", user.getEmail()); // ✅ THÊM

        // Xóa các token cũ của user này
        tokenRepository.deleteByUser(user);

        // Tạo token mới
        String token = java.util.UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(java.time.LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);

        tokenRepository.save(resetToken);
        log.info("💾 Token saved: {}", token); // ✅ THÊM

        // Gửi email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), token);
            log.info("📧 Email sent successfully to: {}", user.getEmail()); // ✅ THÊM
        } catch (Exception e) {
            log.error("❌ Failed to send email: ", e); // ✅ THÊM
            throw new RuntimeException("Không thể gửi email");
        }
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        log.info("🔍 Looking for token: {}", token); // ✅ THÊM

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Token không hợp lệ"));

        log.info("✅ Token found for user: {}", resetToken.getUser().getEmail()); // ✅ THÊM

        if (resetToken.getUsed()) {
            log.warn("⚠️ Token already used"); // ✅ THÊM
            throw new BadRequestException("Token đã được sử dụng");
        }

        if (resetToken.isExpired()) {
            log.warn("⚠️ Token expired"); // ✅ THÊM
            throw new BadRequestException("Token đã hết hạn");
        }

        // Cập nhật mật khẩu
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Đánh dấu token đã sử dụng
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("✅ Password reset successfully for user: {}", user.getEmail()); // ✅ THÊM
    }
}
