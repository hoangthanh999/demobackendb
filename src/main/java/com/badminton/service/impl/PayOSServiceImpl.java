// backend/src/main/java/com/badminton/service/impl/PayOSServiceImpl.java
package com.badminton.service.impl;

import com.badminton.config.PayOSConfig;
import com.badminton.dto.response.PayOSPaymentResponse;
import com.badminton.entity.Order;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.OrderRepository;
import com.badminton.service.PayOSService;
import com.badminton.service.UserTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// ✅ CORRECT imports for PayOS SDK v2.0.1
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
// ❌ REMOVE: import vn.payos.model.v2.paymentRequests.PaymentLinkResponse; // This class does NOT exist

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PayOSServiceImpl implements PayOSService {

    private final PayOSConfig payOSConfig;
    private final OrderRepository orderRepository;
    private final UserTierService userTierService;

    @Override
    public PayOSPaymentResponse createOrderPayment(Long orderId) {
        log.info("🔵 Creating PayOS payment for order: {}", orderId);

        try {
            // 1. Get order
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

            log.info("📦 Order found: {}, Amount: {}", order.getOrderNumber(), order.getTotalAmount());

            // 2. Validate PayOS configuration
            validatePayOSConfig();

            // 3. Initialize PayOS
            PayOS payOS = new PayOS(
                    payOSConfig.getClientId(),
                    payOSConfig.getApiKey(),
                    payOSConfig.getChecksumKey());

            log.info("✅ PayOS initialized");

            // 4. Create unique order code (max 16 digits)
            long orderCode = System.currentTimeMillis() / 1000; // Unix timestamp

            // Save orderCode to order for webhook matching
            if (order.getMomoOrderId() == null || order.getMomoOrderId().isEmpty()) {
                order.setMomoOrderId(String.valueOf(orderCode));
                orderRepository.save(order);
            }

            log.info("📱 Order Code: {}", orderCode);

            // 5. Create description (max 25 chars for PayOS)
            String orderNumberShort = order.getOrderNumber().length() > 20
                    ? order.getOrderNumber().substring(0, 20)
                    : order.getOrderNumber();
            String description = "DH " + orderNumberShort;

            // 6. Build URLs with parameters
            String returnUrl = payOSConfig.getReturnUrl()
                    + "?type=order&orderId=" + orderId
                    + "&orderCode=" + orderCode;

            String cancelUrl = payOSConfig.getCancelUrl()
                    + "?type=order&orderId=" + orderId
                    + "&orderCode=" + orderCode
                    + "&cancel=true";

            log.info("🔗 Return URL: {}", returnUrl);
            log.info("🔗 Cancel URL: {}", cancelUrl);

            // 7. Create payment link request
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(order.getTotalAmount().longValue())
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            // 8. Create payment link via PayOS API
            log.info("📤 Sending request to PayOS...");
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentRequest);

            log.info("✅ PayOS payment link created successfully");
            log.info("🔗 Checkout URL: {}", response.getCheckoutUrl());

            // 9. Return response
            return PayOSPaymentResponse.builder()
                    .checkoutUrl(response.getCheckoutUrl())
                    .orderCode(String.valueOf(orderCode))
                    .amount(order.getTotalAmount().longValue())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating PayOS payment for order {}", orderId, e);
            throw new RuntimeException("Không thể tạo thanh toán PayOS: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void handleWebhook(Map<String, Object> webhookData) {
        log.info("🔔 Processing PayOS webhook: {}", webhookData);

        try {
            // 1. Parse webhook structure
            String code = (String) webhookData.get("code");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            if (data == null) {
                log.warn("⚠️ No data in webhook payload");
                return;
            }

            // 2. Extract payment info
            Long orderCode = getLongValue(data.get("orderCode"));
            String status = (String) data.get("status");
            Integer amount = getIntValue(data.get("amount"));
            String transactionDateTime = (String) data.get("transactionDateTime");

            log.info("📦 Order Code: {}", orderCode);
            log.info("💰 Amount: {}", amount);
            log.info("✅ Status: {}", status);
            log.info("🕐 Transaction Time: {}", transactionDateTime);

            // 3. Find order
            Order order = orderRepository.findByMomoOrderId(String.valueOf(orderCode))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy đơn hàng với orderCode: " + orderCode));

            // 4. Check if already processed (idempotency)
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                log.warn("⚠️ Payment already processed for order: {}", order.getOrderNumber());
                return;
            }

            // 5. Process based on status
            if ("00".equals(code) && "PAID".equals(status)) {
                // ✅ Payment successful
                log.info("✅ Processing successful payment");

                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());

                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                }

                if (transactionDateTime != null) {
                    order.setMomoTransactionId(transactionDateTime);
                }

                orderRepository.save(order);

                // Update user tier
                userTierService.addSpending(order.getUser().getId(), order.getTotalAmount());

                log.info("✅ Payment completed for order: {}", order.getOrderNumber());
                log.info("💳 Transaction ID: {}", transactionDateTime);

            } else if ("CANCELLED".equals(status)) {
                // ❌ Payment cancelled
                log.warn("❌ Payment cancelled");

                order.setPaymentStatus(Order.PaymentStatus.UNPAID);

                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    order.setCancelledAt(LocalDateTime.now());
                    order.setCancelReason("Thanh toán bị hủy");
                }

                orderRepository.save(order);

            } else {
                log.warn("⚠️ Unknown status: {} with code: {}", status, code);
            }

        } catch (Exception e) {
            log.error("❌ Error processing webhook", e);
            throw new RuntimeException("Failed to process webhook: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> handleCallback(String orderCode, String status) {
        log.info("🔙 Processing callback - orderCode: {}, status: {}", orderCode, status);

        try {
            Order order = orderRepository.findByMomoOrderId(orderCode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy đơn hàng với orderCode: " + orderCode));

            boolean isSuccess = order.getPaymentStatus() == Order.PaymentStatus.PAID;

            return Map.of(
                    "success", isSuccess,
                    "orderNumber", order.getOrderNumber(),
                    "orderCode", orderCode,
                    "status", status != null ? status : "UNKNOWN",
                    "paymentStatus", order.getPaymentStatus().toString(),
                    "orderStatus", order.getStatus().toString(),
                    "amount", order.getTotalAmount(),
                    "paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null,
                    "message", isSuccess
                            ? "Thanh toán thành công! Đơn hàng đang được xử lý."
                            : "Thanh toán chưa hoàn tất. Vui lòng kiểm tra lại.");

        } catch (Exception e) {
            log.error("❌ Error processing callback", e);
            return Map.of(
                    "success", false,
                    "message", "Lỗi: " + e.getMessage());
        }
    }

    /**
     * ✅ FIXED: Get payment status from database (not PayOS API)
     * PayOS SDK v2.0.1 doesn't have a simple "get payment info" method
     * So we check our database instead
     */
    @Override
    public Map<String, Object> getPaymentStatus(String orderCode) {
        try {
            log.info("🔍 Checking payment status for orderCode: {}", orderCode);

            // Find order in database
            Order order = orderRepository.findByMomoOrderId(orderCode)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy đơn hàng với orderCode: " + orderCode));

            // Return order payment status
            return Map.of(
                    "orderCode", orderCode,
                    "orderNumber", order.getOrderNumber(),
                    "paymentStatus", order.getPaymentStatus().toString(),
                    "orderStatus", order.getStatus().toString(),
                    "amount", order.getTotalAmount(),
                    "paidAt", order.getPaidAt() != null ? order.getPaidAt().toString() : null,
                    "createdAt", order.getCreatedAt().toString(),
                    "message", order.getPaymentStatus() == Order.PaymentStatus.PAID
                            ? "Đã thanh toán"
                            : "Chưa thanh toán");

        } catch (Exception e) {
            log.error("❌ Error checking payment status", e);
            throw new RuntimeException("Failed to check payment status: " + e.getMessage(), e);
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * Validate PayOS configuration
     */
    private void validatePayOSConfig() {
        if (payOSConfig.getClientId() == null || payOSConfig.getClientId().isEmpty()) {
            throw new RuntimeException("PayOS Client ID is not configured");
        }
        if (payOSConfig.getApiKey() == null || payOSConfig.getApiKey().isEmpty()) {
            throw new RuntimeException("PayOS API Key is not configured");
        }
        if (payOSConfig.getChecksumKey() == null || payOSConfig.getChecksumKey().isEmpty()) {
            throw new RuntimeException("PayOS Checksum Key is not configured");
        }
    }

    /**
     * Convert Object to Long safely
     */
    private Long getLongValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Long)
            return (Long) value;
        if (value instanceof Integer)
            return ((Integer) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse Long from: {}", value);
                return null;
            }
        }
        return null;
    }

    /**
     * Convert Object to Integer safely
     */
    private Integer getIntValue(Object value) {
        if (value == null)
            return null;
        if (value instanceof Integer)
            return (Integer) value;
        if (value instanceof Long)
            return ((Long) value).intValue();
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                log.warn("Cannot parse Integer from: {}", value);
                return null;
            }
        }
        return null;
    }
}