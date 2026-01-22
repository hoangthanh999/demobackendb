// backend/src/main/java/com/badminton/entity/ChatMessage.java
package com.badminton.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages", indexes = {
        @Index(name = "idx_chat_user_created", columnList = "user_id,createdAt")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String aiResponse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType messageType = MessageType.GENERAL;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON: {courtId, productId, bookingId, etc}

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageType {
        GENERAL, // Hỏi đáp chung
        COURT_SEARCH, // Tìm sân
        COURT_BOOKING, // Đặt sân
        PRODUCT_SEARCH, // Tìm sản phẩm
        PRODUCT_ORDER, // Mua hàng
        PAYMENT_QR, // Tạo QR
        LOCATION_UPDATE // Cập nhật vị trí
    }
}
