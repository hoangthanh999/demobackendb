// backend/src/main/java/com/badminton/service/UserTierService.java
package com.badminton.service;

import com.badminton.dto.response.UserTierResponse;
import com.badminton.entity.User;

import java.math.BigDecimal;

public interface UserTierService {
    void updateUserTier(Long userId);

    void addSpending(Long userId, BigDecimal amount);

    UserTierResponse getUserTierInfo(Long userId);

    Integer getDepositPercentage(User user);

    boolean canBookWithoutDeposit(User user);
}
