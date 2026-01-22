// backend/src/main/java/com/badminton/service/QRCodeService.java
package com.badminton.service;

public interface QRCodeService {
    String generatePaymentQR(Long bookingId, String paymentType);

    String generateOrderQR(Long orderId);
}
