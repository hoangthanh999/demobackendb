package com.badminton.controller;

import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.VNPayPaymentResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payments/vnpay")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final VNPayService vnPayService;
    private final UserRepository userRepository;

    @PostMapping("/create-booking/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createBookingPayment(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "DEPOSIT") String paymentType,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("🔵 Creating VNPay payment for booking: {}", bookingId);

        User user = getUserFromAuth(authentication);
        VNPayPaymentResponse response = vnPayService.createPaymentUrl(bookingId, paymentType, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thanh toán VNPay thành công"));
    }

    @PostMapping("/create-order/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<VNPayPaymentResponse>> createOrderPayment(
            @PathVariable Long orderId,
            HttpServletRequest request,
            Authentication authentication) {

        log.info("🔵 Creating VNPay payment for order: {}", orderId);

        User user = getUserFromAuth(authentication);
        VNPayPaymentResponse response = vnPayService.createOrderPaymentUrl(orderId, request);

        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thanh toán VNPay thành công"));
    }

    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<String>> handleCallback(HttpServletRequest request) {
        log.info("📥 VNPay callback received");

        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> {
            params.put(key, value[0]);
        });

        vnPayService.handleCallback(params);

        return ResponseEntity.ok(ApiResponse.success("OK", "Xử lý callback thành công"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
