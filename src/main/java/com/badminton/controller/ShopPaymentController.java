// backend/src/main/java/com/badminton/controller/ShopPaymentController.java
package com.badminton.controller;

import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.ShopMoMoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/shop/payments")
@RequiredArgsConstructor
@Slf4j
public class ShopPaymentController {

    private final ShopMoMoService shopMoMoService;
    private final UserRepository userRepository;

    @PostMapping("/momo/create/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MoMoPaymentResponse>> createMoMoPayment(
            @PathVariable Long orderId,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        MoMoPaymentResponse response = shopMoMoService.createOrderPayment(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(response, "Tạo thanh toán thành công"));
    }

    @PostMapping("/momo/webhook")
    public ResponseEntity<Void> handleMoMoWebhook(@RequestBody MoMoWebhookRequest webhook) {
        log.info("📥 Received MoMo webhook for shop order: {}", webhook);
        shopMoMoService.handleOrderPaymentWebhook(
                webhook.getOrderId(),
                webhook.getResultCode(),
                webhook.getTransId());
        return ResponseEntity.ok().build();
    }

    // ✅ Mock payment confirmation endpoint
    @PostMapping("/mock/confirm/{momoOrderId}")
    public ResponseEntity<ApiResponse<Void>> confirmMockPayment(
            @PathVariable String momoOrderId,
            @RequestParam(defaultValue = "0") int resultCode) {
        log.info("🎭 Mock payment confirmation: momoOrderId={}, resultCode={}", momoOrderId, resultCode);

        shopMoMoService.handleOrderPaymentWebhook(
                momoOrderId,
                resultCode,
                System.currentTimeMillis());

        return ResponseEntity.ok(ApiResponse.success(null,
                resultCode == 0 ? "Thanh toán thành công" : "Thanh toán thất bại"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
