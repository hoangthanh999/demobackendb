package com.badminton.service;

import com.badminton.dto.request.CourtRequest;
import com.badminton.dto.response.CourtResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface CourtService {
    CourtResponse createCourt(CourtRequest request, Long ownerId);

    CourtResponse updateCourt(Long id, CourtRequest request, Long ownerId);

    CourtResponse getCourtById(Long id);

    Page<CourtResponse> getAllCourts(Pageable pageable);

    Page<CourtResponse> searchCourts(String name, String address,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable);

    List<CourtResponse> getCourtsByOwner(Long ownerId);

    void deleteCourt(Long id, Long ownerId);

    void updateCourtStatus(Long id, String status, Long ownerId);
}
