// backend/src/main/java/com/badminton/dto/response/UserTierResponse.java
package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserTierResponse {
    private String tier;
    private BigDecimal totalSpent;
    private BigDecimal nextTierThreshold;
    private Integer depositPercentage;
    private Boolean canBookWithoutDeposit;
    private String tierBenefits;
}
