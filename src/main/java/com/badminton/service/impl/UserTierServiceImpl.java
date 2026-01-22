// backend/src/main/java/com/badminton/service/impl/UserTierServiceImpl.java
package com.badminton.service.impl;

import com.badminton.dto.response.UserTierResponse;
import com.badminton.entity.User;
import com.badminton.exception.ResourceNotFoundException;
import com.badminton.repository.UserRepository;
import com.badminton.service.UserTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserTierServiceImpl implements UserTierService {

    private final UserRepository userRepository;

    @Override
    public void updateUserTier(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        User.UserTier newTier = User.UserTier.fromTotalSpent(user.getTotalSpent());

        if (user.getTier() != newTier) {
            log.info("🎉 User {} tier upgraded: {} -> {}",
                    user.getEmail(), user.getTier(), newTier);

            user.setTier(newTier);
            user.setDepositPercentage(newTier.getDepositPercentage());
            userRepository.save(user);
        }
    }

    @Override
    public void addSpending(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        BigDecimal newTotal = user.getTotalSpent().add(amount);
        user.setTotalSpent(newTotal);

        log.info("💰 User {} spent: {} VND, Total: {} VND",
                user.getEmail(), amount, newTotal);

        userRepository.save(user);
        updateUserTier(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserTierResponse getUserTierInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        User.UserTier currentTier = user.getTier();
        User.UserTier[] allTiers = User.UserTier.values();

        BigDecimal nextTierThreshold = null;
        for (int i = 0; i < allTiers.length - 1; i++) {
            if (allTiers[i] == currentTier) {
                nextTierThreshold = BigDecimal.valueOf(allTiers[i + 1].getMinSpent());
                break;
            }
        }

        String benefits = getTierBenefits(currentTier);

        return UserTierResponse.builder()
                .tier(currentTier.name())
                .totalSpent(user.getTotalSpent())
                .nextTierThreshold(nextTierThreshold)
                .depositPercentage(user.getDepositPercentage())
                .canBookWithoutDeposit(canBookWithoutDeposit(user))
                .tierBenefits(benefits)
                .build();
    }

    @Override
    public Integer getDepositPercentage(User user) {
        return user.getDepositPercentage();
    }

    @Override
    public boolean canBookWithoutDeposit(User user) {
        return user.getTier() == User.UserTier.VIP;
    }

    private String getTierBenefits(User.UserTier tier) {
        return switch (tier) {
            case BRONZE -> "🥉 Cọc 30% khi đặt sân";
            case SILVER -> "🥈 Cọc 25% khi đặt sân";
            case GOLD -> "🥇 Cọc 20% khi đặt sân";
            case PLATINUM -> "💎 Cọc 15% khi đặt sân + Ưu tiên hỗ trợ";
            case DIAMOND -> "💠 Cọc 10% khi đặt sân + Ưu tiên hỗ trợ + Giảm giá 5%";
            case VIP -> "👑 Đặt sân KHÔNG CẦN CỌC + Ưu tiên cao nhất + Giảm giá 10%";
        };
    }
}
