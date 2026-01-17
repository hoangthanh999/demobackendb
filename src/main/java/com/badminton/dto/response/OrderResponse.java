// backend/src/main/java/com/badminton/dto/response/OrderResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private String userPhone;
    private List<OrderItemResponse> items;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discount;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;

    // Shipping info
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String note;

    // Payment info
    private String momoTransactionId;
    private String momoOrderId;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;
    private String cancelReason;
}
