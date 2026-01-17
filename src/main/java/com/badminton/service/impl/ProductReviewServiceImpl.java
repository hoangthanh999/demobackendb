// backend/src/main/java/com/badminton/service/impl/ProductReviewServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.ProductReviewRequest;
import com.badminton.dto.response.ProductReviewResponse;
import com.badminton.entity.Order;
import com.badminton.entity.Product;
import com.badminton.entity.ProductReview;
import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.exception.UnauthorizedException;
import com.badminton.repository.OrderItemRepository;
import com.badminton.repository.OrderRepository;
import com.badminton.repository.ProductRepository;
import com.badminton.repository.ProductReviewRepository;
import com.badminton.repository.UserRepository;
import com.badminton.service.ProductReviewService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    public ProductReviewResponse createReview(Long userId, ProductReviewRequest request) {
        log.info("Creating review for product {} by user {}", request.getProductId(), userId);

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        // Validate product
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Validate order
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        // Check if user owns the order
        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền đánh giá đơn hàng này");
        }

        // Check if order is delivered
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Chỉ có thể đánh giá sau khi đơn hàng đã được giao");
        }

        // Check if product is in the order
        boolean productInOrder = orderItemRepository.existsByOrderIdAndProductId(
                order.getId(), product.getId());

        if (!productInOrder) {
            throw new BadRequestException("Sản phẩm không có trong đơn hàng này");
        }

        // Check if already reviewed
        if (reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, product.getId(), order.getId())) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này cho đơn hàng này rồi");
        }

        // Create review
        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setOrder(order);
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setVerified(true); // Verified because user purchased the product

        // Convert images to JSON
        try {
            if (request.getImages() != null && !request.getImages().isEmpty()) {
                review.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý ảnh đánh giá", e);
        }

        ProductReview saved = reviewRepository.save(review);

        // Update product rating
        updateProductRating(product.getId());

        log.info("Review created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public ProductReviewResponse updateReview(Long reviewId, Long userId, ProductReviewRequest request) {
        log.info("Updating review {} by user {}", reviewId, userId);

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        // Check ownership
        if (!review.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Bạn không có quyền chỉnh sửa đánh giá này");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        // Update images
        try {
            if (request.getImages() != null) {
                review.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý ảnh đánh giá", e);
        }

        ProductReview updated = reviewRepository.save(review);

        // Update product rating
        updateProductRating(review.getProduct().getId());

        log.info("Review updated successfully");

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewResponse getReviewById(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));
        return mapToResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getProductReviews(Long productId, Pageable pageable) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }

        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getProductReviewsByRating(Long productId, Integer rating, Pageable pageable) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Không tìm thấy sản phẩm");
        }

        return reviewRepository.findByProductIdAndRatingOrderByCreatedAtDesc(productId, rating, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductReviewResponse> getUserReviews(Long userId, Pageable pageable) {
        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }

        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public void deleteReview(Long reviewId, Long userId) {
        log.info("Deleting review {} by user {}", reviewId, userId);

        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đánh giá"));

        // Check ownership or admin
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (!review.getUser().getId().equals(userId) && user.getRole() != User.UserRole.ADMIN) {
            throw new UnauthorizedException("Bạn không có quyền xóa đánh giá này");
        }

        Long productId = review.getProduct().getId();
        reviewRepository.delete(review);

        // Update product rating
        updateProductRating(productId);

        log.info("Review deleted successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canUserReviewProduct(Long userId, Long productId, Long orderId) {
        // Check if user has purchased the product in this order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            return false;
        }

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            return false;
        }

        boolean productInOrder = orderItemRepository.existsByOrderIdAndProductId(orderId, productId);
        if (!productInOrder) {
            return false;
        }

        // Check if already reviewed
        return !reviewRepository.existsByUserIdAndProductIdAndOrderId(userId, productId, orderId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductReviewResponse> getLatestVerifiedReviews(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return reviewRepository.findLatestVerifiedReviews(pageable).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private void updateProductRating(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        Double averageRating = reviewRepository.calculateAverageRating(productId);
        Integer reviewCount = reviewRepository.countReviewsByProductId(productId);

        product.setAverageRating(averageRating != null ? averageRating : 0.0);
        product.setReviewCount(reviewCount != null ? reviewCount : 0);

        productRepository.save(product);

        log.info("Updated product {} rating: {} ({} reviews)",
                productId, product.getAverageRating(), product.getReviewCount());
    }

    private ProductReviewResponse mapToResponse(ProductReview review) {
        ProductReviewResponse response = ProductReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFullName())
                .orderId(review.getOrder() != null ? review.getOrder().getId() : null)
                .rating(review.getRating())
                .comment(review.getComment())
                .verified(review.getVerified())
                .createdAt(review.getCreatedAt())
                .build();

        // Parse images
        try {
            if (review.getImages() != null) {
                response.setImages(objectMapper.readValue(review.getImages(),
                        new TypeReference<List<String>>() {
                        }));
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing review images", e);
        }

        return response;
    }
}
