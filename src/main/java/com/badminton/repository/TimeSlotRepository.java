package com.badminton.repository;

import com.badminton.entity.Court;
import com.badminton.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findByCourtAndDate(Court court, LocalDate date);

    @Query("SELECT ts FROM TimeSlot ts WHERE ts.court.id = :courtId " +
            "AND ts.date = :date " +
            "AND ts.status = 'AVAILABLE'")
    List<TimeSlot> findAvailableSlots(
            @Param("courtId") Long courtId,
            @Param("date") LocalDate date);
}
