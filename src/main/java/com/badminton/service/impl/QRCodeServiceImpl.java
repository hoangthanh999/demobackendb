// backend/src/main/java/com/badminton/service/impl/QRCodeServiceImpl.java
package com.badminton.service.impl;

import com.badminton.entity.Booking;
import com.badminton.entity.Order;
import com.badminton.entity.Payment;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.OrderRepository;
import com.badminton.repository.PaymentRepository;
import com.badminton.service.QRCodeService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QRCodeServiceImpl implements QRCodeService {

    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Override
    public String generatePaymentQR(Long bookingId, String paymentType) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking"));

        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));

        BigDecimal amount = "FULL".equalsIgnoreCase(paymentType)
                ? payment.getAmount()
                : payment.getDepositAmount();

        // QR Data format for VietQR or MoMo
        String qrData = String.format(
                "BOOKING_ID:%d|AMOUNT:%s|TYPE:%s|ORDER_ID:%s",
                bookingId,
                amount.toString(),
                paymentType,
                payment.getOrderId());

        return generateQRCodeImage(qrData);
    }

    @Override
    public String generateOrderQR(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        String qrData = String.format(
                "ORDER_ID:%d|ORDER_NUMBER:%s|AMOUNT:%s",
                orderId,
                order.getOrderNumber(),
                order.getTotalAmount().toString());

        return generateQRCodeImage(qrData);
    }

    private String generateQRCodeImage(String data) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, 300, 300, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);

            byte[] qrBytes = outputStream.toByteArray();
            String base64QR = Base64.getEncoder().encodeToString(qrBytes);

            log.info("✅ Generated QR code for data: {}", data.substring(0, Math.min(50, data.length())));

            return "data:image/png;base64," + base64QR;

        } catch (WriterException | IOException e) {
            log.error("❌ Error generating QR code", e);
            throw new RuntimeException("Không thể tạo mã QR", e);
        }
    }
}
