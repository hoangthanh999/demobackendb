// backend/src/main/java/com/badminton/repository/ProductRepository.java
package com.badminton.repository;

import com.badminton.entity.Product;
import com.badminton.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

        Optional<Product> findBySlug(String slug);

        boolean existsByName(String name);

        boolean existsBySlug(String slug);

        // Tìm kiếm theo status
        Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);

        // Tìm kiếm theo category
        Page<Product> findByCategory(ProductCategory category, Pageable pageable);

        Page<Product> findByCategoryAndStatus(ProductCategory category, Product.ProductStatus status,
                        Pageable pageable);

        // Sản phẩm nổi bật
        Page<Product> findByFeaturedAndStatus(Boolean featured, Product.ProductStatus status, Pageable pageable);

        List<Product> findTop8ByFeaturedAndStatusOrderByCreatedAtDesc(Boolean featured, Product.ProductStatus status);

        // Sản phẩm bán chạy
        List<Product> findTop8ByStatusOrderBySoldQuantityDesc(Product.ProductStatus status);

        // Sản phẩm mới
        List<Product> findTop8ByStatusOrderByCreatedAtDesc(Product.ProductStatus status);

        // Tìm kiếm theo brand
        Page<Product> findByBrandAndStatus(String brand, Product.ProductStatus status, Pageable pageable);

        @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.brand IS NOT NULL ORDER BY p.brand")
        List<String> findDistinctBrands();

        @Query("SELECT DISTINCT p.brand FROM Product p WHERE p.status = 'ACTIVE' ORDER BY p.brand")
        List<String> findDistinctActiveBrands();

        // Tìm kiếm nâng cao
        @Query("SELECT p FROM Product p WHERE " +
                        "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                        "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
                        "(:brand IS NULL OR p.brand = :brand) AND " +
                        "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
                        "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
                        "(:featured IS NULL OR p.featured = :featured) AND " +
                        "p.status = 'ACTIVE'")
        Page<Product> searchProducts(
                        @Param("keyword") String keyword,
                        @Param("categoryId") Long categoryId,
                        @Param("brand") String brand,
                        @Param("minPrice") BigDecimal minPrice,
                        @Param("maxPrice") BigDecimal maxPrice,
                        @Param("featured") Boolean featured,
                        Pageable pageable);

        // Sản phẩm liên quan (cùng category, khác id)
        @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId " +
                        "AND p.id != :productId AND p.status = 'ACTIVE' " +
                        "ORDER BY RAND()")
        List<Product> findRelatedProducts(@Param("categoryId") Long categoryId,
                        @Param("productId") Long productId,
                        Pageable pageable);

        // Thống kê
        @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
        Long countActiveProducts();

        @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity <= 10 AND p.status = 'ACTIVE'")
        Integer countLowStockProducts();

        @Query("SELECT COUNT(p) FROM Product p WHERE p.stockQuantity = 0 AND p.status = 'ACTIVE'")
        Integer countOutOfStockProducts();

        // Sản phẩm sắp hết hàng
        @Query("SELECT p FROM Product p WHERE p.stockQuantity <= 10 AND p.stockQuantity > 0 " +
                        "AND p.status = 'ACTIVE' ORDER BY p.stockQuantity ASC")
        List<Product> findLowStockProducts(Pageable pageable);

        // Sản phẩm hết hàng
        @Query("SELECT p FROM Product p WHERE p.stockQuantity = 0 AND p.status = 'ACTIVE'")
        List<Product> findOutOfStockProducts(Pageable pageable);

        Page<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                        String name, String description, Pageable pageable);
}
