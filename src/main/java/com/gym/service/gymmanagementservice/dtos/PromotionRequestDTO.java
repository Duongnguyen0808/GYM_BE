package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PromotionTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PromotionRequestDTO {
    @NotBlank
    private String name;
    @NotNull
    private PromotionTargetType targetType;
    @NotNull
    private Long targetId;
    @NotNull
    private BigDecimal discountPercent;
    @NotNull
    private OffsetDateTime startAt;
    @NotNull
    private OffsetDateTime endAt;
}