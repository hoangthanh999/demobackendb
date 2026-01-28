package com.badminton.config;

import com.badminton.entity.ProductCategory;
import com.badminton.entity.User;
import com.badminton.repository.ProductCategoryRepository;
import com.badminton.repository.ProductRepository;
import com.badminton.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("🌱 Starting Data Seeding...");

        seedUsers();
        seedCategories();

        log.info("✅ Data Seeding Completed!");
    }

    private void seedUsers() {
        // Seed Admin
        if (!userRepository.existsByEmail("admin@badminton.com")) {
            User admin = new User();
            admin.setEmail("admin@badminton.com");
            admin.setPassword(passwordEncoder.encode("admin123")); // Default password
            admin.setFullName("System Admin");
            admin.setPhone("0909000111");
            admin.setRole(User.UserRole.ADMIN);
            admin.setActive(true);
            admin.setAddress("System Hq");
            userRepository.save(admin);
            log.info("👤 Created Admin User: admin@badminton.com / admin123");
        }

        // Seed Customer
        if (!userRepository.existsByEmail("user@example.com")) {
            User user = new User();
            user.setEmail("user@example.com");
            user.setPassword(passwordEncoder.encode("user123"));
            user.setFullName("Demo Customer");
            user.setPhone("0909000222");
            user.setRole(User.UserRole.USER);
            user.setActive(true);
            user.setAddress("123 Demo Street");
            userRepository.save(user);
            log.info("👤 Created Demo User: user@example.com / user123");
        }
    }

    private void seedCategories() {
        if (categoryRepository.count() == 0) {
            createCategory("Vợt Cầu Lông", "rackets", "Các loại vợt chuyên nghiệp", 1);
            createCategory("Giày Cầu Lông", "shoes", "Giày thể thao chuyên dụng", 2);
            createCategory("Quần Áo", "apparel", "Trang phục thi đấu và tập luyện", 3);
            createCategory("Phụ Kiện", "accessories", "Túi, quấn cán, tất...", 4);
            log.info("📁 Created 4 Sample Categories");
        }
    }

    private void createCategory(String name, String slug, String description, int order) {
        ProductCategory category = new ProductCategory();
        category.setName(name);
        category.setSlug(slug);
        category.setDescription(description);
        category.setDisplayOrder(order);
        category.setActive(true);
        categoryRepository.save(category);
    }
}
