// backend/src/main/java/com/badminton/service/impl/LocationServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.UpdateLocationRequest;
import com.badminton.dto.response.CourtResponse;
import com.badminton.entity.Court;
import com.badminton.entity.User;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.CourtRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.LocationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LocationServiceImpl implements LocationService {

    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void updateUserLocation(Long userId, UpdateLocationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        user.setLatitude(request.getLatitude());
        user.setLongitude(request.getLongitude());
        user.setAddress(request.getAddress());
        user.setProvince(request.getProvince());
        user.setDistrict(request.getDistrict());
        user.setWard(request.getWard());

        userRepository.save(user);
        log.info("📍 Updated location for user {}: {}, {}",
                user.getEmail(), request.getLatitude(), request.getLongitude());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourtResponse> findNearbyCourts(Long userId, Double radiusKm) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (user.getLatitude() == null || user.getLongitude() == null) {
            log.warn("⚠️ User {} has no location set", user.getEmail());
            return new ArrayList<>();
        }

        List<Court> allCourts = courtRepository.findByStatus(Court.CourtStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        // TODO: Thêm latitude/longitude vào Court entity
        // Hiện tại trả về tất cả courts, sau này filter theo distance

        return allCourts.stream()
                .map(this::mapToCourtResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        // Haversine formula
        final int R = 6371; // Radius of the earth in km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distance in km
    }

    private CourtResponse mapToCourtResponse(Court court) {
        CourtResponse response = CourtResponse.builder()
                .id(court.getId())
                .name(court.getName())
                .address(court.getAddress())
                .description(court.getDescription())
                .pricePerHour(court.getPricePerHour())
                .numberOfCourts(court.getNumberOfCourts())
                .openTime(court.getOpenTime())
                .closeTime(court.getCloseTime())
                .status(court.getStatus().name())
                .ownerId(court.getOwner().getId())
                .ownerName(court.getOwner().getFullName())
                .createdAt(court.getCreatedAt())
                .build();

        try {
            if (court.getFacilities() != null) {
                response.setFacilities(objectMapper.readValue(court.getFacilities(), List.class));
            }
            if (court.getImages() != null) {
                response.setImages(objectMapper.readValue(court.getImages(), List.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing court data", e);
        }

        return response;
    }
}
