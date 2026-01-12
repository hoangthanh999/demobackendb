package com.badminton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal depositAmount; // Số tiền cọc

    @Column(nullable = false)
    private BigDecimal remainingAmount; // Số tiền còn lại

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType; // FULL hoặc DEPOSIT

    @Column(unique = true)
    private String transactionId;

    @Column(unique = true)
    private String orderId; // MoMo order ID

    @Column(unique = true)
    private String requestId; // MoMo request ID

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    private LocalDateTime expiredAt; // Thời gian hết hạn thanh toán

    public enum PaymentMethod {
        CASH, BANK_TRANSFER, MOMO, VNPAY
    }

    public enum PaymentStatus {
        PENDING, // Chờ thanh toán
        COMPLETED, // Đã thanh toán đủ
        PARTIAL, // Đã cọc, chưa thanh toán hết
        FAILED, // Thanh toán thất bại
        REFUNDED, // Đã hoàn tiền
        EXPIRED // Hết hạn thanh toán
    }

    public enum PaymentType {
        FULL, // Thanh toán toàn bộ
        DEPOSIT // Thanh toán cọc
    }
}
