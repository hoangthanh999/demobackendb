// backend/src/main/java/com/badminton/controller/PayOSController.java
package com.badminton.controller;

import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.PayOSPaymentResponse;
import com.badminton.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/payments/payos")
@RequiredArgsConstructor
@Slf4j
public class PayOSController {

    private final PayOSService payOSService;

    /**
     * Tạo payment link PayOS
     */
    @PostMapping("/create-order/{orderId}")
    public ResponseEntity<PayOSPaymentResponse> createOrderPayment(@PathVariable Long orderId) {
        log.info("🔵 Creating PayOS payment for order: {}", orderId);
        PayOSPaymentResponse response = payOSService.createOrderPayment(orderId);
        return ResponseEntity.ok(response);
    }

    /**
     * ✅ Webhook endpoint - PayOS server gọi khi thanh toán thành công
     * QUAN TRỌNG: Đây là endpoint cập nhật database
     */
    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> handleWebhook(@RequestBody Map<String, Object> webhookData) {
        log.info("🔔 Received PayOS webhook: {}", webhookData);

        try {
            payOSService.handleWebhook(webhookData);

            // ✅ PayOS yêu cầu response format này
            return ResponseEntity.ok(Map.of(
                    "error", 0,
                    "message", "Webhook processed successfully",
                    "data", webhookData));
        } catch (Exception e) {
            log.error("❌ Error processing PayOS webhook", e);
            return ResponseEntity.ok(Map.of(
                    "error", -1,
                    "message", "Webhook processing failed: " + e.getMessage(),
                    "data", null));
        }
    }

    /**
     * ✅ THÊM MỚI: Return URL callback - User redirect về sau khi thanh toán
     * Endpoint này để hiển thị UI cho user
     */
    @GetMapping("/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleCallback(
            @RequestParam String orderCode,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Long orderId) {

        log.info("🔙 PayOS callback - orderCode: {}, status: {}, orderId: {}", orderCode, status, orderId);

        try {
            Map<String, Object> result = payOSService.handleCallback(orderCode, status);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("❌ Error processing callback", e);
            return ResponseEntity.ok(ApiResponse.error("Error: " + e.getMessage()));
        }
    }

    /**
     * ✅ THÊM MỚI: Check payment status manually
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkPaymentStatus(@PathVariable String orderCode) {
        log.info("🔍 Checking PayOS payment status for orderCode: {}", orderCode);

        try {
            Map<String, Object> status = payOSService.getPaymentStatus(orderCode);
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("❌ Error checking payment status", e);
            return ResponseEntity.ok(ApiResponse.error("Failed to check payment status: " + e.getMessage()));
        }
    }
}
