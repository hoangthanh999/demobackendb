// backend/src/main/java/com/badminton/repository/ProductCategoryRepository.java
package com.badminton.repository;

import com.badminton.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findBySlug(String slug);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<ProductCategory> findByActiveOrderByDisplayOrderAsc(Boolean active);

    List<ProductCategory> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT c FROM ProductCategory c WHERE c.active = true ORDER BY c.displayOrder ASC")
    List<ProductCategory> findActiveCategories();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId AND p.status = 'ACTIVE'")
    Integer countActiveProductsByCategory(Long categoryId);
}
