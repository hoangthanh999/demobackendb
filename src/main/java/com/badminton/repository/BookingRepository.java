package com.badminton.repository;

import com.badminton.entity.Booking;
import com.badminton.entity.Court;
import com.badminton.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUser(User user);

    List<Booking> findByCourt(Court court);

    Page<Booking> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    @Query("SELECT b FROM Booking b WHERE b.court.id = :courtId " +
            "AND b.bookingDate = :date " +
            "AND b.courtNumber = :courtNumber " +
            "AND b.status != 'CANCELLED' " +
            "AND ((b.startTime < :endTime AND b.endTime > :startTime))")
    List<Booking> findConflictingBookings(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date,
            @Param("courtNumber") Integer courtNumber,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.court.owner.id = :ownerId " +
            "ORDER BY b.createdAt DESC")
    Page<Booking> findByCourtOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
