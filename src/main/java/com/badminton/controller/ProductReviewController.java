// backend/src/main/java/com/badminton/controller/ProductReviewController.java
package com.badminton.controller;

import com.badminton.dto.request.ProductReviewRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.ProductReviewResponse;
import com.badminton.entity.User;
import com.badminton.repository.UserRepository;
import com.badminton.service.ProductReviewService;
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

import java.util.List;

@RestController
@RequestMapping("/shop/reviews")
@RequiredArgsConstructor
public class ProductReviewController {

    private final ProductReviewService reviewService;
    private final UserRepository userRepository;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductReviewResponse> reviews = reviewService.getProductReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/product/{productId}/rating/{rating}")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getProductReviewsByRating(
            @PathVariable Long productId,
            @PathVariable Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductReviewResponse> reviews = reviewService.getProductReviewsByRating(productId, rating, pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/latest-verified")
    public ResponseEntity<ApiResponse<List<ProductReviewResponse>>> getLatestVerifiedReviews(
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductReviewResponse> reviews = reviewService.getLatestVerifiedReviews(limit);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    // ==================== USER ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @Valid @RequestBody ProductReviewRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        ProductReviewResponse review = reviewService.createReview(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(review, "Đánh giá thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody ProductReviewRequest request,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        ProductReviewResponse review = reviewService.updateReview(id, user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(review, "Cập nhật đánh giá thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        reviewService.deleteReview(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa đánh giá thành công"));
    }

    @GetMapping("/my-reviews")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ProductReviewResponse> reviews = reviewService.getUserReviews(user.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(reviews));
    }

    @GetMapping("/can-review")
    @PreAuthorize("hasAnyRole('USER', 'OWNER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> canReviewProduct(
            @RequestParam Long productId,
            @RequestParam Long orderId,
            Authentication authentication) {
        User user = getUserFromAuth(authentication);
        boolean canReview = reviewService.canUserReviewProduct(user.getId(), productId, orderId);
        return ResponseEntity.ok(ApiResponse.success(canReview));
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }
}
