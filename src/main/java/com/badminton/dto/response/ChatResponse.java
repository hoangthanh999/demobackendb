// backend/src/main/java/com/badminton/dto/response/ChatResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatResponse {
    private Long messageId;
    private String sessionId;
    private String aiResponse;
    private MessageType messageType;
    private Map<String, Object> actionData; // Data cho actions (courts, products, booking, etc)
    private List<QuickAction> quickActions; // Gợi ý actions
    private LocalDateTime timestamp;

    public enum MessageType {
        TEXT, // Text thuần
        COURT_LIST, // Danh sách sân
        PRODUCT_LIST, // Danh sách sản phẩm
        BOOKING_CONFIRM, // Xác nhận đặt sân
        PAYMENT_QR, // QR thanh toán
        ORDER_CONFIRM // Xác nhận đơn hàng
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String label;
        private String action; // BOOK_COURT, BUY_PRODUCT, VIEW_CART, etc
        private Map<String, Object> params;
    }
}
