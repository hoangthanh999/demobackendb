// backend/src/main/java/com/badminton/service/impl/ProductServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.ProductRequest;
import com.badminton.dto.request.ProductSearchRequest;
import com.badminton.dto.response.ProductDetailResponse;
import com.badminton.dto.response.ProductResponse;
import com.badminton.dto.response.ProductReviewResponse;
import com.badminton.entity.Product;
import com.badminton.entity.ProductCategory;
import com.badminton.entity.ProductReview;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.ProductCategoryRepository;
import com.badminton.repository.ProductRepository;
import com.badminton.repository.ProductReviewRepository;
import com.badminton.service.CloudinaryService;
import com.badminton.service.ProductService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductReviewRepository reviewRepository;
    private final CloudinaryService cloudinaryService;
    private final ObjectMapper objectMapper;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating product: {}", request.getName());

        // Validate category
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        if (productRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên sản phẩm đã tồn tại");
        }

        // Validate price
        if (request.getPrice().compareTo(request.getOriginalPrice()) > 0) {
            throw new BadRequestException("Giá bán không được lớn hơn giá gốc");
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setSlug(generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);
        product.setStatus(Product.ProductStatus.ACTIVE);

        // Convert lists/maps to JSON
        try {
            if (request.getImages() != null) {
                product.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
            if (request.getSpecifications() != null) {
                product.setSpecifications(objectMapper.writeValueAsString(request.getSpecifications()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý dữ liệu", e);
        }

        Product saved = productRepository.save(product);
        log.info("Product created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Validate category
        ProductCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        // Check duplicate name (exclude current)
        if (!product.getName().equals(request.getName())
                && productRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên sản phẩm đã tồn tại");
        }

        // Validate price
        if (request.getPrice().compareTo(request.getOriginalPrice()) > 0) {
            throw new BadRequestException("Giá bán không được lớn hơn giá gốc");
        }

        // Delete old images if changed
        try {
            if (request.getImages() != null && product.getImages() != null) {
                List<String> oldImages = objectMapper.readValue(product.getImages(),
                        new TypeReference<List<String>>() {
                        });
                List<String> newImages = request.getImages();

                for (String oldUrl : oldImages) {
                    if (!newImages.contains(oldUrl)) {
                        String publicId = cloudinaryService.extractPublicId(oldUrl);
                        if (publicId != null) {
                            cloudinaryService.deleteImage(publicId);
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing images", e);
        }

        product.setName(request.getName());
        product.setSlug(generateSlug(request.getName()));
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setOriginalPrice(request.getOriginalPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setBrand(request.getBrand());
        product.setFeatured(request.getFeatured() != null ? request.getFeatured() : false);

        try {
            if (request.getImages() != null) {
                product.setImages(objectMapper.writeValueAsString(request.getImages()));
            }
            if (request.getSpecifications() != null) {
                product.setSpecifications(objectMapper.writeValueAsString(request.getSpecifications()));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi xử lý dữ liệu", e);
        }

        Product updated = productRepository.save(product);
        log.info("Product updated successfully");

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        return mapToDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));
        return mapToDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findByStatus(Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductSearchRequest request) {
        Sort sort = Sort.by(
                request.getSortDir() != null && request.getSortDir().equalsIgnoreCase("ASC")
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC,
                request.getSortBy() != null ? request.getSortBy() : "createdAt");

        Pageable pageable = PageRequest.of(
                request.getPage() != null ? request.getPage() : 0,
                request.getSize() != null ? request.getSize() : 20,
                sort);

        return productRepository.searchProducts(
                request.getKeyword(),
                request.getCategoryId(),
                request.getBrand(),
                request.getMinPrice(),
                request.getMaxPrice(),
                request.getFeatured(),
                pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size) {
        ProductCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return productRepository.findByCategoryAndStatus(category, Product.ProductStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts() {
        return productRepository.findTop8ByFeaturedAndStatusOrderByCreatedAtDesc(true, Product.ProductStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getBestSellingProducts() {
        return productRepository.findTop8ByStatusOrderBySoldQuantityDesc(Product.ProductStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewProducts() {
        return productRepository.findTop8ByStatusOrderByCreatedAtDesc(Product.ProductStatus.ACTIVE)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        Pageable pageable = PageRequest.of(0, limit);

        return productRepository.findRelatedProducts(product.getCategory().getId(), productId, pageable)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteProduct(Long id) {
        log.info("Deleting product ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        // Delete images from Cloudinary
        try {
            if (product.getImages() != null) {
                List<String> imageUrls = objectMapper.readValue(product.getImages(),
                        new TypeReference<List<String>>() {
                        });
                for (String imageUrl : imageUrls) {
                    String publicId = cloudinaryService.extractPublicId(imageUrl);
                    if (publicId != null) {
                        cloudinaryService.deleteImage(publicId);
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing images for deletion", e);
        }

        productRepository.delete(product);
        log.info("Product deleted successfully");
    }

    @Override
    public void updateProductStatus(Long id, String status) {
        log.info("Updating product status ID: {} to {}", id, status);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        product.setStatus(Product.ProductStatus.valueOf(status.toUpperCase()));
        productRepository.save(product);
        log.info("Product status updated successfully");
    }

    @Override
    public void updateStock(Long productId, Integer quantity, boolean isIncrease) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm"));

        int newStock = isIncrease
                ? product.getStockQuantity() + quantity
                : product.getStockQuantity() - quantity;

        if (newStock < 0) {
            throw new BadRequestException("Số lượng tồn kho không đủ");
        }

        product.setStockQuantity(newStock);

        // Update status based on stock
        if (newStock == 0) {
            product.setStatus(Product.ProductStatus.OUT_OF_STOCK);
        } else if (product.getStatus() == Product.ProductStatus.OUT_OF_STOCK) {
            product.setStatus(Product.ProductStatus.ACTIVE);
        }

        productRepository.save(product);
        log.info("Product stock updated: {} -> {}", productId, newStock);
    }

    private ProductResponse mapToResponse(Product product) {
        ProductResponse response = ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(calculateDiscountPercent(product.getOriginalPrice(), product.getPrice()))
                .stockQuantity(product.getStockQuantity())
                .soldQuantity(product.getSoldQuantity())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brand(product.getBrand())
                .status(product.getStatus().name())
                .featured(product.getFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();

        try {
            if (product.getImages() != null) {
                response.setImages(objectMapper.readValue(product.getImages(),
                        new TypeReference<List<String>>() {
                        }));
            }
            if (product.getSpecifications() != null) {
                response.setSpecifications(objectMapper.readValue(product.getSpecifications(),
                        new TypeReference<Map<String, String>>() {
                        }));
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
        }

        return response;
    }

    private ProductDetailResponse mapToDetailResponse(Product product) {
        // Get recent reviews
        List<ProductReviewResponse> recentReviews = reviewRepository
                .findTop5ByProductIdOrderByCreatedAtDesc(product.getId())
                .stream()
                .map(this::mapReviewToResponse)
                .collect(Collectors.toList());

        // Get related products
        List<ProductResponse> relatedProducts = getRelatedProducts(product.getId(), 4);

        ProductDetailResponse response = ProductDetailResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercent(calculateDiscountPercent(product.getOriginalPrice(), product.getPrice()))
                .stockQuantity(product.getStockQuantity())
                .soldQuantity(product.getSoldQuantity())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brand(product.getBrand())
                .status(product.getStatus().name())
                .featured(product.getFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .recentReviews(recentReviews)
                .relatedProducts(relatedProducts)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();

        try {
            if (product.getImages() != null) {
                response.setImages(objectMapper.readValue(product.getImages(),
                        new TypeReference<List<String>>() {
                        }));
            }
            if (product.getSpecifications() != null) {
                response.setSpecifications(objectMapper.readValue(product.getSpecifications(),
                        new TypeReference<Map<String, String>>() {
                        }));
            }
        } catch (JsonProcessingException e) {
            log.error("Error processing JSON", e);
        }

        return response;
    }

    private ProductReviewResponse mapReviewToResponse(ProductReview review) {
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

    private Integer calculateDiscountPercent(BigDecimal originalPrice, BigDecimal price) {
        if (originalPrice.compareTo(price) <= 0) {
            return 0;
        }
        BigDecimal discount = originalPrice.subtract(price);
        BigDecimal percent = discount.divide(originalPrice, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return percent.intValue();
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        String originalSlug = slug;
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
