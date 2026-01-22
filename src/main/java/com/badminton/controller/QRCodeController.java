// backend/src/main/java/com/badminton/controller/QRCodeController.java
package com.badminton.controller;

import com.badminton.dto.response.ApiResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Order;
import com.badminton.entity.User;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.OrderRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.QRCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qr")
@RequiredArgsConstructor
@Slf4j
public class QRCodeController {

    private final QRCodeService qrCodeService;
    private final BookingRepository bookingRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    @GetMapping("/booking/{bookingId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> generateBookingQR(
            @PathVariable Long bookingId,
            @RequestParam(defaultValue = "DEPOSIT") String paymentType,
            Authentication authentication) {

        log.info("🎫 Generating QR for booking {}, type: {}", bookingId, paymentType);

        User user = getUserFromAuth(authentication);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy booking"));

        if (!booking.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Bạn không có quyền tạo QR cho booking này");
        }

        String qrCode = qrCodeService.generatePaymentQR(bookingId, paymentType);

        return ResponseEntity.ok(ApiResponse.success(qrCode, "Đã tạo mã QR thanh toán"));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> generateOrderQR(
            @PathVariable Long orderId,
            Authentication authentication) {

        log.info("🎫 Generating QR for order {}", orderId);

        User user = getUserFromAuth(authentication);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("Bạn không có quyền tạo QR cho đơn hàng này");
        }

        String qrCode = qrCodeService.generateOrderQR(orderId);

        return ResponseEntity.ok(ApiResponse.success(qrCode, "Đã tạo mã QR thanh toán"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
