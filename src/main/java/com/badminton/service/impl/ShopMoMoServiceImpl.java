// backend/src/main/java/com/badminton/service/impl/ShopMoMoServiceImpl.java
package com.badminton.service.impl;

import com.badminton.config.MoMoConfig;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.entity.Order;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.OrderRepository;
import com.badminton.service.OrderService;
import com.badminton.service.ShopMoMoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ShopMoMoServiceImpl implements ShopMoMoService {

    private final MoMoConfig moMoConfig;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Value("${payment.mock.enabled:true}")
    private boolean mockEnabled;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public MoMoPaymentResponse createOrderPayment(Long orderId, Long userId) {
        log.info("🔵 Creating MoMo payment for order: {}", orderId);

        // Get order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check permission
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền thanh toán đơn hàng này");
        }

        // Check order status
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new BadRequestException("Đơn hàng đã bị hủy");
        }

        if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            throw new BadRequestException("Đơn hàng đã được thanh toán");
        }

        // Check payment method
        if (order.getPaymentMethod() != Order.PaymentMethod.MOMO) {
            throw new BadRequestException("Đơn hàng không sử dụng phương thức thanh toán MoMo");
        }

        String momoOrderId = "SHOP_" + order.getOrderNumber() + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // Save MoMo info to order
        order.setMomoOrderId(momoOrderId);
        order.setMomoRequestId(requestId);
        orderRepository.save(order);

        // Mock mode
        if (mockEnabled) {
            log.info("🎭 Mock mode enabled - generating mock payment URL");
            String mockPayUrl = generateMockPayUrl(momoOrderId);

            return MoMoPaymentResponse.builder()
                    .payUrl(mockPayUrl)
                    .orderId(momoOrderId)
                    .requestId(requestId)
                    .message("Mock payment created successfully")
                    .resultCode(0)
                    .build();
        }

        // Real MoMo payment
        return createRealMoMoPayment(order, momoOrderId, requestId);
    }

    @Override
    public void handleOrderPaymentWebhook(String momoOrderId, Integer resultCode, Long transId) {
        log.info("📥 Handling MoMo webhook for order: {}, resultCode: {}", momoOrderId, resultCode);

        Order order = orderRepository.findByMomoOrderId(momoOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (resultCode == 0) {
            // Payment success
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            order.setMomoTransactionId(
                    transId != null ? transId.toString() : "MOCK_TRANS_" + System.currentTimeMillis());

            // Update order status
            if (order.getStatus() == Order.OrderStatus.PENDING) {
                order.setStatus(Order.OrderStatus.CONFIRMED);
            }

            orderRepository.save(order);
            log.info("✅ Order payment completed successfully");
        } else {
            // Payment failed
            log.warn("❌ Order payment failed with resultCode: {}", resultCode);
            // Optionally cancel the order or keep it pending
        }
    }

    private MoMoPaymentResponse createRealMoMoPayment(Order order, String momoOrderId, String requestId) {
        try {
            Map<String, Object> momoRequest = new HashMap<>();
            momoRequest.put("partnerCode", moMoConfig.getPartnerCode());
            momoRequest.put("accessKey", moMoConfig.getAccessKey());
            momoRequest.put("requestId", requestId);
            momoRequest.put("amount", order.getTotalAmount().longValue());
            momoRequest.put("orderId", momoOrderId);
            momoRequest.put("orderInfo", "Thanh toán đơn hàng " + order.getOrderNumber());
            momoRequest.put("redirectUrl", frontendUrl + "/shop/orders/" + order.getId() + "/payment-result");
            momoRequest.put("ipnUrl",
                    moMoConfig.getIpnUrl().replace("/payments/momo/webhook", "/shop/payments/momo/webhook"));
            momoRequest.put("requestType", moMoConfig.getRequestType());
            momoRequest.put("extraData", order.getId().toString());
            momoRequest.put("lang", "vi");

            // Create signature
            String rawSignature = "accessKey=" + moMoConfig.getAccessKey() +
                    "&amount=" + order.getTotalAmount().longValue() +
                    "&extraData=" + order.getId() +
                    "&ipnUrl=" + momoRequest.get("ipnUrl") +
                    "&orderId=" + momoOrderId +
                    "&orderInfo=" + momoRequest.get("orderInfo") +
                    "&partnerCode=" + moMoConfig.getPartnerCode() +
                    "&redirectUrl=" + momoRequest.get("redirectUrl") +
                    "&requestId=" + requestId +
                    "&requestType=" + moMoConfig.getRequestType();

            String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, moMoConfig.getSecretKey())
                    .hmacHex(rawSignature);
            momoRequest.put("signature", signature);

            // Call MoMo API
            String jsonRequest = objectMapper.writeValueAsString(momoRequest);
            log.info("📤 MoMo Request: {}", jsonRequest);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(moMoConfig.getEndpoint());
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));

                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.info("📥 MoMo Response: {}", responseBody);

                    Map<String, Object> momoResponse = objectMapper.readValue(responseBody, Map.class);

                    return MoMoPaymentResponse.builder()
                            .payUrl((String) momoResponse.get("payUrl"))
                            .orderId(momoOrderId)
                            .requestId(requestId)
                            .message((String) momoResponse.get("message"))
                            .resultCode((Integer) momoResponse.get("resultCode"))
                            .build();
                }
            }

        } catch (Exception e) {
            log.error("❌ Error creating MoMo payment", e);
            throw new BadRequestException("Không thể tạo thanh toán: " + e.getMessage());
        }
    }

    private String generateMockPayUrl(String momoOrderId) {
        return String.format("mock://shop-payment?orderId=%s&resultCode=0", momoOrderId);
    }
}
