// backend/src/main/java/com/badminton/service/impl/ProductCategoryServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.request.ProductCategoryRequest;
import com.badminton.dto.response.ProductCategoryResponse;
import com.badminton.entity.ProductCategory;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.ProductCategoryRepository;
import com.badminton.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    @Override
    public ProductCategoryResponse createCategory(ProductCategoryRequest request) {
        log.info("Creating product category: {}", request.getName());

        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Danh mục đã tồn tại");
        }

        ProductCategory category = new ProductCategory();
        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());
        category.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        category.setActive(true);

        ProductCategory saved = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", saved.getId());

        return mapToResponse(saved);
    }

    @Override
    public ProductCategoryResponse updateCategory(Long id, ProductCategoryRequest request) {
        log.info("Updating category ID: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        // Check duplicate name (exclude current)
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Tên danh mục đã tồn tại");
        }

        category.setName(request.getName());
        category.setSlug(generateSlug(request.getName()));
        category.setDescription(request.getDescription());
        category.setImageUrl(request.getImageUrl());

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        ProductCategory updated = categoryRepository.save(category);
        log.info("Category updated successfully");

        return mapToResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategoryById(Long id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductCategoryResponse getCategoryBySlug(String slug) {
        ProductCategory category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));
        return mapToResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductCategoryResponse> getActiveCategories() {
        return categoryRepository.findActiveCategories().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Deleting category ID: {}", id);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        Integer productCount = categoryRepository.countActiveProductsByCategory(id);
        if (productCount > 0) {
            throw new BadRequestException("Không thể xóa danh mục đang có " + productCount + " sản phẩm");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully");
    }

    @Override
    public void updateCategoryStatus(Long id, Boolean active) {
        log.info("Updating category status ID: {} to {}", id, active);

        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy danh mục"));

        category.setActive(active);
        categoryRepository.save(category);
        log.info("Category status updated successfully");
    }

    private ProductCategoryResponse mapToResponse(ProductCategory category) {
        Integer productCount = categoryRepository.countActiveProductsByCategory(category.getId());

        return ProductCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .active(category.getActive())
                .displayOrder(category.getDisplayOrder())
                .productCount(productCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    private String generateSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");

        // Check unique slug
        String originalSlug = slug;
        int counter = 1;
        while (categoryRepository.existsBySlug(slug)) {
            slug = originalSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
