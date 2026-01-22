// backend/src/main/java/com/badminton/controller/LocationController.java
package com.badminton.controller;

import com.badminton.dto.request.UpdateLocationRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.CourtResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.LocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/location")
@RequiredArgsConstructor
@Slf4j
public class LocationController {

    private final LocationService locationService;
    private final UserRepository userRepository;

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @Valid @RequestBody UpdateLocationRequest request,
            Authentication authentication) {

        log.info("📍 Updating location: {}, {}", request.getLatitude(), request.getLongitude());

        User user = getUserFromAuth(authentication);
        locationService.updateUserLocation(user.getId(), request);

        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật vị trí thành công"));
    }

    @GetMapping("/nearby-courts")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getNearbyCourts(
            @RequestParam(defaultValue = "10.0") Double radiusKm,
            Authentication authentication) {

        User user = getUserFromAuth(authentication);
        List<CourtResponse> courts = locationService.findNearbyCourts(user.getId(), radiusKm);

        return ResponseEntity.ok(ApiResponse.success(courts,
                "Tìm thấy " + courts.size() + " sân gần bạn"));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
