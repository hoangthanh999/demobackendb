package com.badminton.service.impl;

import com.badminton.config.VNPayConfig;
import com.badminton.dto.response.VNPayPaymentResponse;


import com.badminton.entity.Booking;
import com.badminton.entity.Order;
import com.badminton.entity.Payment;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.OrderRepository;
import com.badminton.repository.PaymentRepository;
import com.badminton.service.OrderService;
import com.badminton.service.UserTierService;
import com.badminton.service.VNPayService;
import com.badminton.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class VNPayServiceImpl implements VNPayService {

    private final VNPayConfig vnPayConfig;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final UserTierService userTierService;
    private final OrderService orderService;

    @Override
    public VNPayPaymentResponse createPaymentUrl(Long bookingId, String paymentType, HttpServletRequest request) {
        log.info("🔵 Creating VNPay payment for booking: {}", bookingId);

        // 1. Validate booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy booking"));

        // 2. Get payment
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));

        // 3. Calculate amount (VNPay yêu cầu đơn vị VND * 100)
        BigDecimal amount = "FULL".equalsIgnoreCase(paymentType)
                ? payment.getAmount()
                : payment.getDepositAmount();

        long vnpAmount = amount.multiply(BigDecimal.valueOf(100)).longValue();

        // 4. Create VNPay params
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");

        String vnpTxnRef = "BOOKING_" + bookingId + "_" + System.currentTimeMillis();
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan dat san " + booking.getCourt().getName());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // 5. Create date (yyyyMMddHHmmss)
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        // Expire after 15 minutes
        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // 6. Build hash data and create secure hash
        String hashData = VNPayUtil.buildHashData(vnpParams);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", vnpSecureHash);

        // 7. Build payment URL
        String queryUrl = VNPayUtil.buildQueryString(vnpParams);
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl;

        log.info("✅ VNPay payment URL created: {}", paymentUrl);

        return VNPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(vnpTxnRef)
                .amount(amount)
                .build();
    }

    @Override
    public VNPayPaymentResponse createOrderPaymentUrl(Long orderId, HttpServletRequest request) {
        log.info("🔵 Creating VNPay payment for order: {}", orderId);

        // 1. Get order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // 2. Calculate amount
        long vnpAmount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        // 3. Create VNPay params
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");

        String vnpTxnRef = "ORDER_" + order.getOrderNumber() + "_" + System.currentTimeMillis();
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderNumber());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl() + "?type=order");
        vnpParams.put("vnp_IpAddr", VNPayUtil.getIpAddress(request));

        // 4. Create date
        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnpCreateDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_CreateDate", vnpCreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnpExpireDate = formatter.format(cld.getTime());
        vnpParams.put("vnp_ExpireDate", vnpExpireDate);

        // 5. Create secure hash
        String hashData = VNPayUtil.buildHashData(vnpParams);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        vnpParams.put("vnp_SecureHash", vnpSecureHash);

        // 6. Build payment URL
        String queryUrl = VNPayUtil.buildQueryString(vnpParams);
        String paymentUrl = vnPayConfig.getUrl() + "?" + queryUrl;

        // 7. Save VNPay info to order
        order.setMomoOrderId(vnpTxnRef); // Tạm dùng field này
        orderRepository.save(order);

        log.info("✅ VNPay payment URL created for order: {}", paymentUrl);

        return VNPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .txnRef(vnpTxnRef)
                .amount(order.getTotalAmount())
                .build();
    }

    @Override
    public void handleCallback(Map<String, String> params) {
        log.info("📥 Received VNPay callback: {}", params);

        // 1. Verify signature
        if (!verifySignature(params)) {
            log.error("❌ Invalid VNPay signature");
            throw new RuntimeException("Invalid signature");
        }

        // 2. Get transaction info
        String vnpTxnRef = params.get("vnp_TxnRef");
        String vnpResponseCode = params.get("vnp_ResponseCode");
        String vnpTransactionNo = params.get("vnp_TransactionNo");
        String vnpAmount = params.get("vnp_Amount");

        log.info("Transaction: txnRef={}, responseCode={}, transNo={}",
                vnpTxnRef, vnpResponseCode, vnpTransactionNo);

        // 3. Process based on type
        if (vnpTxnRef.startsWith("BOOKING_")) {
            handleBookingPayment(vnpTxnRef, vnpResponseCode, vnpTransactionNo);
        } else if (vnpTxnRef.startsWith("ORDER_")) {
            handleOrderPayment(vnpTxnRef, vnpResponseCode, vnpTransactionNo);
        }
    }

    @Override
    public boolean verifySignature(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        String hashData = VNPayUtil.buildHashData(params);
        String calculatedHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        return calculatedHash.equals(vnpSecureHash);
    }

    // ==================== PRIVATE METHODS ====================

    private void handleBookingPayment(String vnpTxnRef, String responseCode, String transactionNo) {
        try {
            // Extract booking ID from txnRef
            String[] parts = vnpTxnRef.split("_");
            Long bookingId = Long.parseLong(parts[1]);

            Payment payment = paymentRepository.findByBookingId(bookingId)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy payment"));

            if ("00".equals(responseCode)) {
                // Payment success
                payment.setTransactionId(transactionNo);
                payment.setPaidAt(LocalDateTime.now());
                payment.setStatus(payment.getPaymentType() == Payment.PaymentType.FULL
                        ? Payment.PaymentStatus.COMPLETED
                        : Payment.PaymentStatus.PARTIAL);

                Booking booking = payment.getBooking();
                booking.setStatus(Booking.BookingStatus.CONFIRMED);

                paymentRepository.save(payment);
                bookingRepository.save(booking);

                // Update user spending
                BigDecimal paidAmount = payment.getPaymentType() == Payment.PaymentType.FULL
                        ? payment.getAmount()
                        : payment.getDepositAmount();
                userTierService.addSpending(booking.getUser().getId(), paidAmount);

                log.info("✅ Booking payment completed: {}", bookingId);
            } else {
                // Payment failed
                payment.setStatus(Payment.PaymentStatus.FAILED);
                payment.getBooking().setStatus(Booking.BookingStatus.CANCELLED);

                paymentRepository.save(payment);
                bookingRepository.save(payment.getBooking());

                log.warn("❌ Booking payment failed: {}, code: {}", bookingId, responseCode);
            }
        } catch (Exception e) {
            log.error("❌ Error handling booking payment", e);
        }
    }

    private void handleOrderPayment(String vnpTxnRef, String responseCode, String transactionNo) {
        try {
            Order order = orderRepository.findByMomoOrderId(vnpTxnRef)
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

            if ("00".equals(responseCode)) {
                // Payment success
                order.setPaymentStatus(Order.PaymentStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                order.setMomoTransactionId(transactionNo);

                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                }

                orderRepository.save(order);

                // Update user spending
                userTierService.addSpending(order.getUser().getId(), order.getTotalAmount());

                log.info("✅ Order payment completed: {}", order.getOrderNumber());
            } else {
                // Payment failed
                log.warn("❌ Order payment failed: {}, code: {}", order.getOrderNumber(), responseCode);
            }
        } catch (Exception e) {
            log.error("❌ Error handling order payment", e);
        }
    }
}
