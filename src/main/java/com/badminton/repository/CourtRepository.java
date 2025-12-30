package com.badminton.repository;

import com.badminton.entity.Court;
import com.badminton.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    List<Court> findByOwner(User owner);

    Page<Court> findByStatus(Court.CourtStatus status, Pageable pageable);

    @Query("SELECT c FROM Court c WHERE " +
            "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
            "(:address IS NULL OR LOWER(c.address) LIKE LOWER(CONCAT('%', :address, '%'))) AND " +
            "(:minPrice IS NULL OR c.pricePerHour >= :minPrice) AND " +
            "(:maxPrice IS NULL OR c.pricePerHour <= :maxPrice) AND " +
            "c.status = 'ACTIVE'")
    Page<Court> searchCourts(
            @Param("name") String name,
            @Param("address") String address,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);
}
