package com.badminton.controller;

import com.badminton.dto.request.CourtRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.CourtResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;
    private final UserRepository userRepository;

    // Public endpoints
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CourtResponse>>> getAllCourts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<CourtResponse> courts = courtService.getAllCourts(pageable);
        return ResponseEntity.ok(ApiResponse.success(courts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CourtResponse>> getCourtById(@PathVariable Long id) {
        CourtResponse court = courtService.getCourtById(id);
        return ResponseEntity.ok(ApiResponse.success(court));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<CourtResponse>>> searchCourts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<CourtResponse> courts = courtService.searchCourts(name, address, minPrice, maxPrice, pageable);
        return ResponseEntity.ok(ApiResponse.success(courts));
    }

    // Owner endpoints
    @PostMapping
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourtResponse>> createCourt(
            @Valid @RequestBody CourtRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CourtResponse court = courtService.createCourt(request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(court, "Tạo sân thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CourtResponse>> updateCourt(
            @PathVariable Long id,
            @Valid @RequestBody CourtRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        CourtResponse court = courtService.updateCourt(id, request, user.getId());
        return ResponseEntity.ok(ApiResponse.success(court, "Cập nhật sân thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCourt(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        courtService.deleteCourt(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa sân thành công"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateCourtStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        courtService.updateCourtStatus(id, status, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công"));
    }

    @GetMapping("/my-courts")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CourtResponse>>> getMyCourts(
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        List<CourtResponse> courts = courtService.getCourtsByOwner(user.getId());
        return ResponseEntity.ok(ApiResponse.success(courts));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
