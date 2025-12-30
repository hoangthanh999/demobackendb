package com.badminton.service;

import com.badminton.dto.request.BookingRequest;
import com.badminton.dto.response.BookingResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface BookingService {
    BookingResponse createBooking(BookingRequest request, Long userId);

    Page<BookingResponse> getAllBookings(Pageable pageable);

    BookingResponse getBookingById(Long id, Long userId);

    Page<BookingResponse> getUserBookings(Long userId, Pageable pageable);

    Page<BookingResponse> getOwnerBookings(Long ownerId, Pageable pageable);

    BookingResponse updateBookingStatus(Long id, String status, Long userId);

    void cancelBooking(Long id, Long userId);

    List<BookingResponse> getCourtBookings(Long courtId, Long ownerId);
}
