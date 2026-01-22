// backend/src/main/java/com/badminton/dto/response/ChatResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    private Map<String, Object> actionData;
    private List<QuickAction> quickActions;

    // ✅ THÊM FORMAT CHO TIMESTAMP
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    public enum MessageType {
        TEXT, COURT_LIST, PRODUCT_LIST, BOOKING_CONFIRM, PAYMENT_QR, ORDER_CONFIRM
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuickAction {
        private String label;
        private String action;
        private Map<String, Object> params;
    }
}
