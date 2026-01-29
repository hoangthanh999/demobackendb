// backend/src/main/java/com/badminton/service/PayOSService.java
package com.badminton.service;

import com.badminton.dto.response.PayOSPaymentResponse;

import java.util.Map;

public interface PayOSService {

    /**
     * Tạo payment link PayOS
     */
    PayOSPaymentResponse createOrderPayment(Long orderId);

    /**
     * Xử lý webhook từ PayOS server (IPN)
     */
    void handleWebhook(Map<String, Object> webhookData);

    /**
     * ✅ THÊM MỚI: Xử lý callback khi user quay lại
     */
    Map<String, Object> handleCallback(String orderCode, String status);

    /**
     * ✅ THÊM MỚI: Kiểm tra trạng thái thanh toán
     */
    Map<String, Object> getPaymentStatus(String orderCode);
}
