package com.badminton.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CourtResponse {
    private Long id;
    private String name;
    private String address;
    private String description;
    private BigDecimal pricePerHour;
    private Integer numberOfCourts;
    private List<String> facilities;
    private List<String> images;
    private String openTime;
    private String closeTime;
    private String status;
    private Long ownerId;
    private String ownerName;
    private LocalDateTime createdAt;
}
