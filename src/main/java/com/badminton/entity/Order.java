// backend/src/main/java/com/badminton/entity/Order.java
package com.badminton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_user", columnList = "user_id"),
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_number", columnList = "orderNumber"),
        @Index(name = "idx_order_created", columnList = "createdAt"),
        @Index(name = "idx_order_momo", columnList = "momoOrderId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String orderNumber; // ORD_20240117_001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private BigDecimal subtotal; // Tổng tiền hàng

    @Column(nullable = false)
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal discount = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal totalAmount; // Tổng thanh toán

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    // Thông tin giao hàng
    @Column(nullable = false)
    private String recipientName;

    @Column(nullable = false)
    private String recipientPhone;

    @Column(nullable = false)
    private String shippingAddress;

    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;

    @Column(columnDefinition = "TEXT")
    private String note;

    // Thông tin thanh toán MoMo
    private String momoTransactionId;
    private String momoOrderId;
    private String momoRequestId;

    private LocalDateTime paidAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    @Column(columnDefinition = "TEXT")
    private String cancelReason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        PENDING, // Chờ xác nhận
        CONFIRMED, // Đã xác nhận
        PROCESSING, // Đang xử lý
        SHIPPING, // Đang giao
        DELIVERED, // Đã giao
        CANCELLED, // Đã hủy
        RETURNED // Đã trả hàng
    }

    public enum PaymentMethod {
        COD, // Thanh toán khi nhận hàng
        MOMO, // MoMo
        BANK_TRANSFER, // Chuyển khoản
        ONLINE // Thanh toán online (VNPay, MoMo, etc.)
    }

    public enum PaymentStatus {
        UNPAID, // Chưa thanh toán
        PAID, // Đã thanh toán
        REFUNDED // Đã hoàn tiền
    }
}
