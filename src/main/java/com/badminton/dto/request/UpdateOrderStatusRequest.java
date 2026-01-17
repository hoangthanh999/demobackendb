// backend/src/main/java/com/badminton/dto/request/UpdateOrderStatusRequest.java
package com.badminton.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotBlank(message = "Trạng thái không được để trống")
    private String status; // CONFIRMED, PROCESSING, SHIPPING, DELIVERED, CANCELLED

    private String cancelReason; // Bắt buộc nếu status = CANCELLED
}
