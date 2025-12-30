package com.badminton.controller;

import com.badminton.dto.request.BookingRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.BookingResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        BookingResponse booking = bookingService.createBooking(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(booking, "Đặt sân thành công"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        BookingResponse booking = bookingService.getBookingById(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(booking));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BookingResponse> bookings = bookingService.getUserBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/owner-bookings")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getOwnerBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BookingResponse> bookings = bookingService.getOwnerBookings(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @GetMapping("/court/{courtId}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getCourtBookings(
            @PathVariable Long courtId,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        List<BookingResponse> bookings = bookingService.getCourtBookings(courtId, user.getId());
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<BookingResponse>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        BookingResponse booking = bookingService.updateBookingStatus(id, status, user.getId());
        return ResponseEntity.ok(ApiResponse.success(booking, "Cập nhật trạng thái thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        bookingService.cancelBooking(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Hủy đặt sân thành công"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    // ✅ THÊM endpoint mới cho ADMIN
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<BookingResponse>>> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<BookingResponse> bookings = bookingService.getAllBookings(pageable);
        return ResponseEntity.ok(ApiResponse.success(bookings));
    }

}
