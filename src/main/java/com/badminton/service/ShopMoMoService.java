// backend/src/main/java/com/badminton/service/ShopMoMoService.java
package com.badminton.service;

import com.badminton.dto.response.MoMoPaymentResponse;

public interface ShopMoMoService {
    MoMoPaymentResponse createOrderPayment(Long orderId, Long userId);

    void handleOrderPaymentWebhook(String momoOrderId, Integer resultCode, Long transId);
}
