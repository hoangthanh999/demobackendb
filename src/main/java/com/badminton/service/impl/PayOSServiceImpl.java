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
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;

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

            // 2. Initialize PayOS
            PayOS payOS = new PayOS(
                    payOSConfig.getClientId(),
                    payOSConfig.getApiKey(),
                    payOSConfig.getChecksumKey());

            // 3. ✅ FIX: Create order code - Kiểm tra null và tạo mới nếu cần
            long orderCode;
            if (order.getMomoOrderId() != null && !order.getMomoOrderId().isEmpty()) {
                try {
                    orderCode = Long.parseLong(order.getMomoOrderId());
                } catch (NumberFormatException e) {
                    // Nếu không parse được, tạo mới
                    orderCode = System.currentTimeMillis() / 1000;
                    order.setMomoOrderId(String.valueOf(orderCode));
                }
            } else {
                // Tạo mới nếu null
                orderCode = System.currentTimeMillis() / 1000;
                order.setMomoOrderId(String.valueOf(orderCode));
            }

            // ✅ FIX: Giới hạn description 25 ký tự
            String orderNumberShort = order.getOrderNumber().length() > 20
                    ? order.getOrderNumber().substring(0, 20)
                    : order.getOrderNumber();
            String description = "DH " + orderNumberShort; // Max 23 chars

            String returnUrl = payOSConfig.getReturnUrl() + "?type=order&orderId=" + orderId;
            String cancelUrl = payOSConfig.getCancelUrl() + "?type=order&orderId=" + orderId;

            // 4. Create payment link request (v2 API)
            CreatePaymentLinkRequest paymentRequest = CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(order.getTotalAmount().longValue())
                    .description(description)
                    .returnUrl(returnUrl)
                    .cancelUrl(cancelUrl)
                    .build();

            // 5. Create payment link
            CreatePaymentLinkResponse response = payOS.paymentRequests().create(paymentRequest);

            // 6. Save PayOS order code to order
            orderRepository.save(order);

            log.info("✅ PayOS payment URL created: {}", response.getCheckoutUrl());

            return PayOSPaymentResponse.builder()
                    .checkoutUrl(response.getCheckoutUrl())
                    .orderCode(String.valueOf(orderCode))
                    .amount(order.getTotalAmount().longValue())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error creating PayOS payment", e);
            throw new RuntimeException("Không thể tạo thanh toán PayOS: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(Map<String, Object> webhookData) {
        log.info("📥 Received PayOS webhook: {}", webhookData);

        try {
            // 1. Get webhook data
            String code = (String) webhookData.get("code");
            String desc = (String) webhookData.get("desc");

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) webhookData.get("data");

            if (data == null) {
                log.warn("⚠️ No data in webhook");
                return;
            }

            Long orderCode = ((Number) data.get("orderCode")).longValue();
            String status = (String) data.get("status");

            log.info("PayOS webhook - orderCode: {}, status: {}, code: {}", orderCode, status, code);

            // 2. Find order
            Order order = orderRepository.findByMomoOrderId(String.valueOf(orderCode))
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

            // 3. Process based on status
            if ("00".equals(code) && "PAID".equals(status)) {
                // Payment success
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());

                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                }

                // Save transaction ID if available
                if (data.get("transactionDateTime") != null) {
                    order.setMomoTransactionId(data.get("transactionDateTime").toString());
                }

                orderRepository.save(order);

                // Update user spending
                userTierService.addSpending(order.getUser().getId(), order.getTotalAmount());

                log.info("✅ PayOS payment completed for order: {}", order.getOrderNumber());
            } else {
                // Payment failed or cancelled
                log.warn("❌ PayOS payment failed for order: {}, status: {}", order.getOrderNumber(), status);
            }

        } catch (Exception e) {
            log.error("❌ Error handling PayOS webhook", e);
        }
    }
}
