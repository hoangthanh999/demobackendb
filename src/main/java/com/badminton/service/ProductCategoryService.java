// backend/src/main/java/com/badminton/service/ProductCategoryService.java
package com.badminton.service;

import com.badminton.dto.request.ProductCategoryRequest;
import com.badminton.dto.response.ProductCategoryResponse;

import java.util.List;

public interface ProductCategoryService {
    ProductCategoryResponse createCategory(ProductCategoryRequest request);

    ProductCategoryResponse updateCategory(Long id, ProductCategoryRequest request);

    ProductCategoryResponse getCategoryById(Long id);

    ProductCategoryResponse getCategoryBySlug(String slug);

    List<ProductCategoryResponse> getAllCategories();

    List<ProductCategoryResponse> getActiveCategories();

    void deleteCategory(Long id);

    void updateCategoryStatus(Long id, Boolean active);
}
