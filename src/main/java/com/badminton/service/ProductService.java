// backend/src/main/java/com/badminton/service/ProductService.java
package com.badminton.service;

import com.badminton.dto.request.ProductRequest;
import com.badminton.dto.request.ProductSearchRequest;
import com.badminton.dto.response.ProductDetailResponse;
import com.badminton.dto.response.ProductResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    ProductDetailResponse getProductById(Long id);

    ProductDetailResponse getProductBySlug(String slug);

    Page<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    Page<ProductResponse> searchProducts(ProductSearchRequest request);

    Page<ProductResponse> getProductsByCategory(Long categoryId, int page, int size);

    List<ProductResponse> getFeaturedProducts();

    List<ProductResponse> getBestSellingProducts();

    List<ProductResponse> getNewProducts();

    List<ProductResponse> getRelatedProducts(Long productId, int limit);

    void deleteProduct(Long id);

    void updateProductStatus(Long id, String status);

    void updateStock(Long productId, Integer quantity, boolean isIncrease);
}
