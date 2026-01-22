// controller/UserController.java
package com.badminton.controller;

import com.badminton.dto.request.ChangePasswordRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.security.UserPrincipal;
import com.badminton.service.UserService;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

        private final UserService userService;

        @PutMapping("/change-password")
        public ResponseEntity<ApiResponse<Void>> changePassword(
                        @AuthenticationPrincipal UserPrincipal userPrincipal, // ✅ Dùng UserPrincipal
                        @Valid @RequestBody ChangePasswordRequest request) {

                log.info("📝 Change password request from user: {} (ID: {})",
                                userPrincipal.getEmail(), userPrincipal.getId());

                // ✅ Lấy userId trực tiếp từ UserPrincipal
                userService.changePassword(userPrincipal.getId(), request);

                return ResponseEntity.ok(ApiResponse.<Void>builder()
                                .success(true)
                                .message("Đổi mật khẩu thành công")
                                .build());
        }
}
