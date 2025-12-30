package com.badminton.service.impl;

import com.badminton.dto.request.CourtRequest;
import com.badminton.dto.response.CourtResponse;
import com.badminton.entity.Court;
import com.badminton.entity.User;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.CourtRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.CourtService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CourtServiceImpl implements CourtService {

    private final CourtRepository courtRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CourtResponse createCourt(CourtRequest request, Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ sân"));

        if (owner.getRole() != User.UserRole.OWNER && owner.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedException("Bạn không có quyền tạo sân");
        }

        Court court = new Court();
        court.setName(request.getName());
        court.setAddress(request.getAddress());
        court.setDescription(request.getDescription());
        court.setPricePerHour(request.getPricePerHour());
        court.setNumberOfCourts(request.getNumberOfCourts());
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());
        court.setOwner(owner);
        court.setStatus(Court.CourtStatus.ACTIVE);

        // Convert List to JSON String
        try {
            if (request.getFacilities() != null) {
                court.setFacilities(objectMapper.writeValueAsString(request.getFacilities()));
            }
            if (request.getImages() != null) {
                court.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý dữ liệu", e);
        }

        Court savedCourt = courtRepository.save(court);
        return mapToCourtResponse(savedCourt);
    }

    @Override
    public CourtResponse updateCourt(Long id, CourtRequest request, Long ownerId) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

        if (!court.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật sân này");
        }

        court.setName(request.getName());
        court.setAddress(request.getAddress());
        court.setDescription(request.getDescription());
        court.setPricePerHour(request.getPricePerHour());
        court.setNumberOfCourts(request.getNumberOfCourts());
        court.setOpenTime(request.getOpenTime());
        court.setCloseTime(request.getCloseTime());

        try {
            if (request.getFacilities() != null) {
                court.setFacilities(objectMapper.writeValueAsString(request.getFacilities()));
            }
            if (request.getImages() != null) {
                court.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý dữ liệu", e);
        }

        Court updatedCourt = courtRepository.save(court);
        return mapToCourtResponse(updatedCourt);
    }

    @Override
    public CourtResponse getCourtById(Long id) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));
        return mapToCourtResponse(court);
    }

    @Override
    public Page<CourtResponse> getAllCourts(Pageable pageable) {
        return courtRepository.findByStatus(Court.CourtStatus.ACTIVE, pageable)
                .map(this::mapToCourtResponse);
    }

    @Override
    public Page<CourtResponse> searchCourts(String name, String address,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable) {
        return courtRepository.searchCourts(name, address, minPrice, maxPrice, pageable)
                .map(this::mapToCourtResponse);
    }

    @Override
    public List<CourtResponse> getCourtsByOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy chủ sân"));

        return courtRepository.findByOwner(owner).stream()
                .map(this::mapToCourtResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCourt(Long id, Long ownerId) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

        if (!court.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("Bạn không có quyền xóa sân này");
        }

        courtRepository.delete(court);
    }

    @Override
    public void updateCourtStatus(Long id, String status, Long ownerId) {
        Court court = courtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sân"));

        if (!court.getOwner().getId().equals(ownerId)) {
            throw new UnauthorizedException("Bạn không có quyền cập nhật trạng thái sân này");
        }

        court.setStatus(Court.CourtStatus.valueOf(status.toUpperCase()));
        courtRepository.save(court);
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

        // Convert JSON String to List
        try {
            if (court.getFacilities() != null) {
                response.setFacilities(objectMapper.readValue(court.getFacilities(), List.class));
            }
            if (court.getImages() != null) {
                response.setImages(objectMapper.readValue(court.getImages(), List.class));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý dữ liệu", e);
        }

        return response;
    }
}
