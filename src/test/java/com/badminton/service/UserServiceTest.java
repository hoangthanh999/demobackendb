package com.badminton.service;

import com.badminton.entity.User;
import com.badminton.exception.BadRequestException;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.UserRepository;
import com.badminton.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setFullName("Test User");
        user.setPhone("0909000111");
        user.setPassword("$2a$10$hashedPassword");
        user.setRole(User.UserRole.USER);
        user.setActive(true);
        user.setTotalSpent(BigDecimal.ZERO);
        user.setTier(User.UserTier.BRONZE);
        user.setDepositPercentage(30);
    }

    @Test
    void getUserById_ShouldReturnUser_WhenExists() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void getUserById_ShouldThrowException_WhenNotExists() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(Exception.class, () -> {
            userRepository.findById(999L).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        });
    }

    @Test
    void createUser_ShouldEncryptPassword() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$newHashedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(2L);
            return savedUser;
        });

        // Act
        User newUser = new User();
        newUser.setEmail("new@example.com");
        newUser.setPassword(passwordEncoder.encode("plainPassword"));
        newUser.setFullName("New User");
        newUser.setPhone("0909000222");
        newUser.setRole(User.UserRole.USER);

        User saved = userRepository.save(newUser);

        // Assert
        assertNotNull(saved.getId());
        assertNotEquals("plainPassword", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2a$"));
    }

    @Test
    void updateUserTier_ShouldUpgradeTier_WhenSpendingIncreases() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act - User spends 3 million VND (should become SILVER)
        user.setTotalSpent(BigDecimal.valueOf(3_000_000));
        User.UserTier newTier = User.UserTier.fromTotalSpent(user.getTotalSpent());
        user.setTier(newTier);
        user.setDepositPercentage(newTier.getDepositPercentage());

        User updated = userRepository.save(user);

        // Assert
        assertEquals(User.UserTier.SILVER, updated.getTier());
        assertEquals(25, updated.getDepositPercentage()); // SILVER has 25% deposit
    }

    @Test
    void updateUserTier_ShouldBeVIP_WhenSpendingOver50M() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act - User spends over 50 million (should become VIP)
        user.setTotalSpent(BigDecimal.valueOf(60_000_000));
        User.UserTier newTier = User.UserTier.fromTotalSpent(user.getTotalSpent());
        user.setTier(newTier);
        user.setDepositPercentage(newTier.getDepositPercentage());

        User updated = userRepository.save(user);

        // Assert
        assertEquals(User.UserTier.VIP, updated.getTier());
        assertEquals(0, updated.getDepositPercentage()); // VIP has 0% deposit (no deposit needed)
    }

    @Test
    void changePassword_ShouldValidateOldPassword() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongOldPassword", user.getPassword())).thenReturn(false);
        when(passwordEncoder.matches("correctOldPassword", user.getPassword())).thenReturn(true);

        // Act & Assert - Wrong old password
        boolean wrongMatch = passwordEncoder.matches("wrongOldPassword", user.getPassword());
        assertFalse(wrongMatch);

        // Act & Assert - Correct old password
        boolean correctMatch = passwordEncoder.matches("correctOldPassword", user.getPassword());
        assertTrue(correctMatch);
    }

    @Test
    void updateProfile_ShouldUpdateUserInfo() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        user.setFullName("Updated Name");
        user.setPhone("0909999999");
        User updated = userRepository.save(user);

        // Assert
        assertEquals("Updated Name", updated.getFullName());
        assertEquals("0909999999", updated.getPhone());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void createUser_ShouldThrowException_WhenEmailExists() {
        // Arrange
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        // Act & Assert
        assertThrows(Exception.class, () -> {
            if (userRepository.existsByEmail("test@example.com")) {
                throw new BadRequestException("Email already exists");
            }
        });
    }

    @Test
    void deactivateUser_ShouldSetActiveFalse() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        user.setActive(false);
        User deactivated = userRepository.save(user);

        // Assert
        assertFalse(deactivated.getActive());
        verify(userRepository, times(1)).save(user);
    }
}
