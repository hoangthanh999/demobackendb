// backend/src/main/java/com/badminton/service/ProductReviewService.java
package com.badminton.service;

import com.badminton.dto.request.ProductReviewRequest;
import com.badminton.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductReviewService {
    ProductReviewResponse createReview(Long userId, ProductReviewRequest request);

    ProductReviewResponse updateReview(Long reviewId, Long userId, ProductReviewRequest request);

    ProductReviewResponse getReviewById(Long reviewId);

    Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable);

    Page<ProductReviewResponse> getProductReviewsByRating(Long productId, Integer rating, Pageable pageable);

    Page<ProductReviewResponse> getUserReviews(Long userId, Pageable pageable);

    void deleteReview(Long reviewId, Long userId);

    boolean canUserReviewProduct(Long userId, Long productId, Long orderId);

    List<ProductReviewResponse> getLatestVerifiedReviews(int limit);
}
