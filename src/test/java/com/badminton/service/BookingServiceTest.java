package com.badminton.service;

import com.badminton.dto.request.BookingRequest;
import com.badminton.dto.response.BookingResponse;
import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.repository.BookingRepository;
import com.badminton.repository.CourtRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.impl.BookingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingServiceImpl bookingService;

    private User testUser;
    private Court testCourt;
    private BookingRequest bookingRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        // Setup test court
        testCourt = new Court();
        testCourt.setId(1L);
        testCourt.setName("Test Court");
        testCourt.setPricePerHour(BigDecimal.valueOf(100000));
        testCourt.setNumberOfCourts(5);
        testCourt.setOpenTime("06:00");
        testCourt.setCloseTime("22:00");
        testCourt.setStatus(Court.CourtStatus.ACTIVE);
        testCourt.setOwner(testUser);

        // Setup booking request
        bookingRequest = new BookingRequest();
        bookingRequest.setCourtId(1L);
        bookingRequest.setBookingDate(LocalDate.now().plusDays(1));
        bookingRequest.setStartTime("08:00");
        bookingRequest.setEndTime("10:00");
        bookingRequest.setCourtNumber(1);
    }

    @Test
    void createBooking_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));
        when(bookingRepository.findConflictingBookings(any(), any(), any(), any(), any()))
                .thenReturn(new ArrayList<>());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(i -> {
            Booking booking = i.getArgument(0);
            booking.setId(1L);
            return booking;
        });

        // When
        BookingResponse response = bookingService.createBooking(bookingRequest, 1L);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Court", response.getCourtName());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    @Test
    void createBooking_ConflictDetected_ThrowsException() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));

        Booking conflictBooking = new Booking();
        when(bookingRepository.findConflictingBookings(any(), any(), any(), any(), any()))
                .thenReturn(List.of(conflictBooking));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(bookingRequest, 1L);
        });

        verify(bookingRepository, never()).save(any(Booking.class));
    }

    @Test
    void createBooking_InvalidTimeRange_ThrowsException() {
        // Given
        bookingRequest.setStartTime("10:00");
        bookingRequest.setEndTime("08:00"); // End before start

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(bookingRequest, 1L);
        });
    }

    @Test
    void createBooking_OutsideOperatingHours_ThrowsException() {
        // Given
        bookingRequest.setStartTime("23:00");
        bookingRequest.setEndTime("24:00");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(courtRepository.findById(1L)).thenReturn(Optional.of(testCourt));

        // When & Then
        assertThrows(BadRequestException.class, () -> {
            bookingService.createBooking(bookingRequest, 1L);
        });
    }
}
