package com.badminton.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOSPaymentResponse {
    private String checkoutUrl;
    private String orderCode;
    private Long amount;
}
