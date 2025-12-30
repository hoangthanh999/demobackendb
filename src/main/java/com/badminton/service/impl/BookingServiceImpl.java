package com.badminton.service.impl;

import com.badminton.dto.request.BookingRequest;
import com.badminton.dto.response.BookingResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.CourtRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final CourtRepository courtRepository;
    private final UserRepository userRepository;

    @Override
    public BookingResponse createBooking(BookingRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        Court court = courtRepository.findById(request.getCourtId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

        // Validate booking date
        if (request.getBookingDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Không thể đặt sân cho ngày trong quá khứ");
        }

        // Validate time
        LocalTime startTime = LocalTime.parse(request.getStartTime());
        LocalTime endTime = LocalTime.parse(request.getEndTime());
        LocalTime openTime = LocalTime.parse(court.getOpenTime());
        LocalTime closeTime = LocalTime.parse(court.getCloseTime());

        if (startTime.isBefore(openTime) || endTime.isAfter(closeTime)) {
            throw new BadRequestException("Thời gian đặt sân không hợp lệ");
        }

        if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
            throw new BadRequestException("Thời gian bắt đầu phải trước thời gian kết thúc");
        }

        // Validate court number
        if (request.getCourtNumber() < 1 || request.getCourtNumber() > court.getNumberOfCourts()) {
            throw new BadRequestException("Số sân không hợp lệ");
        }

        // Check for conflicts
        List<Booking> conflicts = bookingRepository.findConflictingBookings(
                court.getId(),
                request.getBookingDate(),
                request.getCourtNumber(),
                startTime,
                endTime);

        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Sân đã được đặt trong khung giờ này");
        }

        // Calculate total price
        long hours = Duration.between(startTime, endTime).toHours();
        BigDecimal totalPrice = court.getPricePerHour().multiply(BigDecimal.valueOf(hours));

        // Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setBookingDate(request.getBookingDate());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setCourtNumber(request.getCourtNumber());
        booking.setTotalPrice(totalPrice);
        booking.setNotes(request.getNotes());
        booking.setStatus(Booking.BookingStatus.PENDING);

        Booking savedBooking = bookingRepository.save(booking);
        return mapToBookingResponse(savedBooking);
    }

    @Override
    public BookingResponse getBookingById(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

        if (!booking.getUser().getId().equals(userId) &&
                !booking.getCourt().getOwner().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền xem đặt sân này");
        }

        return mapToBookingResponse(booking);
    }

    @Override
    public Page<BookingResponse> getUserBookings(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        return bookingRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::mapToBookingResponse);
    }

    @Override
    public Page<BookingResponse> getOwnerBookings(Long ownerId, Pageable pageable) {
        return bookingRepository.findByCourtOwnerId(ownerId, pageable)
                .map(this::mapToBookingResponse);
    }

    @Override
    public BookingResponse updateBookingStatus(Long id, String status, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // ✅ SỬA: Cho phép ADMIN cập nhật bất kỳ booking nào
        boolean isOwner = booking.getCourt().getOwner().getId().equals(userId);
        boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật trạng thái đặt sân này");
        }

        booking.setStatus(Booking.BookingStatus.valueOf(status.toUpperCase()));
        Booking updatedBooking = bookingRepository.save(booking);
        return mapToBookingResponse(updatedBooking);
    }

    @Override
    public void cancelBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đặt sân"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // ✅ SỬA: Cho phép user hủy booking của mình, hoặc ADMIN/OWNER hủy bất kỳ
        // booking nào
        boolean isBookingOwner = booking.getUser().getId().equals(userId);
        boolean isCourtOwner = booking.getCourt().getOwner().getId().equals(userId);
        boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

        if (!isBookingOwner && !isCourtOwner && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền hủy đặt sân này");
        }

        if (booking.getStatus() == Booking.BookingStatus.COMPLETED) {
            throw new BadRequestException("Không thể hủy đặt sân đã hoàn thành");
        }

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    @Override
    public List<BookingResponse> getCourtBookings(Long courtId, Long ownerId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // ✅ SỬA: Cho phép ADMIN xem bookings của bất kỳ sân nào
        boolean isOwner = court.getOwner().getId().equals(ownerId);
        boolean isAdmin = user.getRole() == User.UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("Bạn không có quyền xem đặt sân của sân này");
        }

        return bookingRepository.findByCourt(court).stream()
                .map(this::mapToBookingResponse)
                .collect(Collectors.toList());
    }

    private BookingResponse mapToBookingResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .userId(booking.getUser().getId())
                .userName(booking.getUser().getFullName())
                .userPhone(booking.getUser().getPhone())
                .courtId(booking.getCourt().getId())
                .courtName(booking.getCourt().getName())
                .courtAddress(booking.getCourt().getAddress())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime().toString())
                .endTime(booking.getEndTime().toString())
                .courtNumber(booking.getCourtNumber())
                .totalPrice(booking.getTotalPrice())
                .status(booking.getStatus().name())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .build();
    }

    @Override
    public Page<BookingResponse> getAllBookings(Pageable pageable) {
        return bookingRepository.findAll(pageable)
                .map(this::mapToBookingResponse);
    }

}
