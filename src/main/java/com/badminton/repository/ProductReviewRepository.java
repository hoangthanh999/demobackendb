// backend/src/main/java/com/badminton/repository/ProductReviewRepository.java
package com.badminton.repository;

import com.badminton.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    // Tìm review theo product
    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    List<ProductReview> findTop5ByProductIdOrderByCreatedAtDesc(Long productId);

    // Tìm review theo user
    Page<ProductReview> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Tìm review theo order
    List<ProductReview> findByOrderId(Long orderId);

    // Kiểm tra đã review chưa
    boolean existsByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    Optional<ProductReview> findByUserIdAndProductIdAndOrderId(Long userId, Long productId, Long orderId);

    // Tìm review verified
    Page<ProductReview> findByProductIdAndVerifiedOrderByCreatedAtDesc(Long productId, Boolean verified,
            Pageable pageable);

    // Tìm theo rating
    Page<ProductReview> findByProductIdAndRatingOrderByCreatedAtDesc(Long productId, Integer rating, Pageable pageable);

    // Thống kê rating
    @Query("SELECT AVG(r.rating) FROM ProductReview r WHERE r.product.id = :productId")
    Double calculateAverageRating(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM ProductReview r WHERE r.product.id = :productId")
    Integer countReviewsByProductId(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM ProductReview r " +
            "WHERE r.product.id = :productId GROUP BY r.rating ORDER BY r.rating DESC")
    List<Object[]> countReviewsByRating(@Param("productId") Long productId);

    // Review mới nhất
    @Query("SELECT r FROM ProductReview r WHERE r.verified = true " +
            "ORDER BY r.createdAt DESC")
    List<ProductReview> findLatestVerifiedReviews(Pageable pageable);
}
