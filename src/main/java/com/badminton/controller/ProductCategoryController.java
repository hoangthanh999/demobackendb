// backend/src/main/java/com/badminton/controller/ProductCategoryController.java
package com.badminton.controller;

import com.badminton.dto.request.ProductCategoryRequest;
import com.badminton.dto.response.ApiResponse;
import com.badminton.dto.response.ProductCategoryResponse;
import com.badminton.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shop/categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService categoryService;

    // ==================== PUBLIC ENDPOINTS ====================

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> getAllCategories() {
        List<ProductCategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ProductCategoryResponse>>> getActiveCategories() {
        List<ProductCategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> getCategoryById(@PathVariable Long id) {
        ProductCategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> getCategoryBySlug(@PathVariable String slug) {
        ProductCategoryResponse category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    // ==================== ADMIN ENDPOINTS ====================

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> createCategory(
            @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.ok(ApiResponse.success(category, "Tạo danh mục thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody ProductCategoryRequest request) {
        ProductCategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Cập nhật danh mục thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Xóa danh mục thành công"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateCategoryStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        categoryService.updateCategoryStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success(null, "Cập nhật trạng thái thành công"));
    }
}
