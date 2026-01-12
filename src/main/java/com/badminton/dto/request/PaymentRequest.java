package com.badminton.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Booking ID không được để trống")
    private Long bookingId;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentType paymentType; // FULL hoặc DEPOSIT

    private String returnUrl; // URL frontend để redirect sau khi thanh toán

    public enum PaymentType {
        FULL, // Thanh toán toàn bộ
        DEPOSIT // Thanh toán cọc
    }
}
