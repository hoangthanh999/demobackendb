// backend/src/main/java/com/badminton/dto/request/ChatRequest.java
package com.badminton.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {

    @NotBlank(message = "Tin nhắn không được để trống")
    private String message;

    private String sessionId; // Optional: để maintain context

    private Double latitude; // Optional: vị trí hiện tại
    private Double longitude;
}
