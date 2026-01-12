package com.badminton.service.impl;

import com.badminton.config.MoMoConfig;
import com.badminton.dto.request.MoMoWebhookRequest;
import com.badminton.dto.request.PaymentRequest;
import com.badminton.dto.response.MoMoPaymentResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Payment;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.PaymentRepository;
import com.badminton.service.MoMoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MoMoServiceImpl implements MoMoService {

    private final MoMoConfig moMoConfig;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    @Value("${payment.deposit-percentage}")
    private Integer depositPercentage;

    @Override
    public MoMoPaymentResponse createPayment(PaymentRequest request, Long userId) {
        // Lấy booking
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

        // Kiểm tra quyền
        if (!booking.getUser().getId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền thanh toán đặt sân này");
        }

        // Kiểm tra trạng thái booking
        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new BadRequestException("Đặt sân này không thể thanh toán");
        }

        // Kiểm tra đã có payment chưa
        paymentRepository.findByBooking(booking).ifPresent(payment -> {
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED ||
                    payment.getStatus() == Payment.PaymentStatus.PARTIAL) {
                throw new BadRequestException("Đặt sân này đã được thanh toán");
            }
        });

        // Tính số tiền cần thanh toán
        BigDecimal totalAmount = booking.getTotalPrice();
        BigDecimal paymentAmount;
        BigDecimal depositAmount;
        BigDecimal remainingAmount;

        if (request.getPaymentType() == PaymentRequest.PaymentType.DEPOSIT) {
            // Thanh toán cọc
            depositAmount = totalAmount
                    .multiply(BigDecimal.valueOf(depositPercentage))
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            paymentAmount = depositAmount;
            remainingAmount = totalAmount.subtract(depositAmount);
        } else {
            // Thanh toán toàn bộ
            depositAmount = totalAmount;
            paymentAmount = totalAmount;
            remainingAmount = BigDecimal.ZERO;
        }

        // Tạo payment record
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(totalAmount);
        payment.setDepositAmount(depositAmount);
        payment.setRemainingAmount(remainingAmount);
        payment.setPaymentMethod(Payment.PaymentMethod.MOMO);
        payment.setPaymentType(request.getPaymentType() == PaymentRequest.PaymentType.DEPOSIT
                ? Payment.PaymentType.DEPOSIT
                : Payment.PaymentType.FULL);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setExpiredAt(LocalDateTime.now().plusMinutes(15)); // Hết hạn sau 15 phút

        String orderId = "BOOKING_" + booking.getId() + "_" + System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        payment.setOrderId(orderId);
        payment.setRequestId(requestId);

        Payment savedPayment = paymentRepository.save(payment);

        // Tạo request đến MoMo
        try {
            Map<String, Object> momoRequest = new HashMap<>();
            momoRequest.put("partnerCode", moMoConfig.getPartnerCode());
            momoRequest.put("accessKey", moMoConfig.getAccessKey());
            momoRequest.put("requestId", requestId);
            momoRequest.put("amount", paymentAmount.longValue());
            momoRequest.put("orderId", orderId);
            momoRequest.put("orderInfo", "Thanh toán đặt sân " + booking.getCourt().getName() +
                    " - " + booking.getBookingDate());
            momoRequest.put("redirectUrl", request.getReturnUrl() != null
                    ? request.getReturnUrl()
                    : moMoConfig.getRedirectUrl());
            momoRequest.put("ipnUrl", moMoConfig.getIpnUrl());
            momoRequest.put("requestType", moMoConfig.getRequestType());
            momoRequest.put("extraData", savedPayment.getId().toString());
            momoRequest.put("lang", "vi");

            // Tạo signature
            String rawSignature = "accessKey=" + moMoConfig.getAccessKey() +
                    "&amount=" + paymentAmount.longValue() +
                    "&extraData=" + savedPayment.getId() +
                    "&ipnUrl=" + moMoConfig.getIpnUrl() +
                    "&orderId=" + orderId +
                    "&orderInfo=" + momoRequest.get("orderInfo") +
                    "&partnerCode=" + moMoConfig.getPartnerCode() +
                    "&redirectUrl=" + momoRequest.get("redirectUrl") +
                    "&requestId=" + requestId +
                    "&requestType=" + moMoConfig.getRequestType();

            String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, moMoConfig.getSecretKey())
                    .hmacHex(rawSignature);
            momoRequest.put("signature", signature);

            // Gọi API MoMo
            String jsonRequest = objectMapper.writeValueAsString(momoRequest);
            log.info("MoMo Request: {}", jsonRequest);

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(moMoConfig.getEndpoint());
                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setEntity(new StringEntity(jsonRequest, "UTF-8"));

                try (CloseableHttpResponse response = client.execute(httpPost)) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    log.info("MoMo Response: {}", responseBody);

                    Map<String, Object> momoResponse = objectMapper.readValue(responseBody, Map.class);

                    return MoMoPaymentResponse.builder()
                            .payUrl((String) momoResponse.get("payUrl"))
                            .orderId(orderId)
                            .requestId(requestId)
                            .message((String) momoResponse.get("message"))
                            .resultCode((Integer) momoResponse.get("resultCode"))
                            .build();
                }
            }

        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            throw new BadRequestException("Không thể tạo thanh toán: " + e.getMessage());
        }
    }

    @Override
    public void handleWebhook(MoMoWebhookRequest webhook) {
        log.info("Received MoMo webhook: {}", webhook);

        // Verify signature
        if (!verifySignature(webhook)) {
            log.error("Invalid signature from MoMo webhook");
            throw new BadRequestException("Invalid signature");
        }

        // Tìm payment
        Payment payment = paymentRepository.findByOrderId(webhook.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thanh toán"));

        // Kiểm tra resultCode
        if (webhook.getResultCode() == 0) {
            // Thanh toán thành công
            payment.setTransactionId(webhook.getTransId().toString());
            payment.setPaidAt(LocalDateTime.now());

            if (payment.getPaymentType() == Payment.PaymentType.FULL) {
                payment.setStatus(Payment.PaymentStatus.COMPLETED);
                payment.getBooking().setStatus(Booking.BookingStatus.CONFIRMED);
            } else {
                payment.setStatus(Payment.PaymentStatus.PARTIAL);
                payment.getBooking().setStatus(Booking.BookingStatus.CONFIRMED);
            }

            paymentRepository.save(payment);
            bookingRepository.save(payment.getBooking());

            log.info("Payment completed successfully for orderId: {}", webhook.getOrderId());
        } else {
            // Thanh toán thất bại
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.getBooking().setStatus(Booking.BookingStatus.CANCELLED);

            paymentRepository.save(payment);
            bookingRepository.save(payment.getBooking());

            log.warn("Payment failed for orderId: {}, resultCode: {}",
                    webhook.getOrderId(), webhook.getResultCode());
        }
    }

    @Override
    public boolean verifySignature(MoMoWebhookRequest webhook) {
        String rawSignature = "accessKey=" + moMoConfig.getAccessKey() +
                "&amount=" + webhook.getAmount() +
                "&extraData=" + webhook.getExtraData() +
                "&message=" + webhook.getMessage() +
                "&orderId=" + webhook.getOrderId() +
                "&orderInfo=" + webhook.getOrderInfo() +
                "&orderType=" + webhook.getOrderType() +
                "&partnerCode=" + webhook.getPartnerCode() +
                "&payType=" + webhook.getPayType() +
                "&requestId=" + webhook.getRequestId() +
                "&responseTime=" + webhook.getResponseTime() +
                "&resultCode=" + webhook.getResultCode() +
                "&transId=" + webhook.getTransId();

        String signature = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, moMoConfig.getSecretKey())
                .hmacHex(rawSignature);

        return signature.equals(webhook.getSignature());
    }
}
