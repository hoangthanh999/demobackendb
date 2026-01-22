// backend/src/main/java/com/badminton/controller/UserTierController.java
package com.badminton.controller;

import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.UserTierResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.UserTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user-tier")
@RequiredArgsConstructor
@Slf4j
public class UserTierController {

    private final UserTierService userTierService;
    private final UserRepository userRepository;

    @GetMapping("/info")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserTierResponse>> getTierInfo(
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        UserTierResponse tierInfo = userTierService.getUserTierInfo(user.getId());

        return ResponseEntity.ok(ApiResponse.success(tierInfo));
    }

    @PostMapping("/refresh")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<UserTierResponse>> refreshTier(
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        userTierService.updateUserTier(user.getId());
        UserTierResponse tierInfo = userTierService.getUserTierInfo(user.getId());

        return ResponseEntity.ok(ApiResponse.success(tierInfo, "Đã cập nhật cấp bậc"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
