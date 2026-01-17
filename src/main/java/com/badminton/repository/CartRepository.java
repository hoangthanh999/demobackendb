// backend/src/main/java/com/badminton/repository/CartRepository.java
package com.badminton.repository;

import com.badminton.entity.Cart;
import com.badminton.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId")
    Integer countItemsByUserId(Long userId);
}
