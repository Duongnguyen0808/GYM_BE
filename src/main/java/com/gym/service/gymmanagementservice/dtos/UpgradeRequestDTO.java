package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpgradeRequestDTO {
    @NotNull
    private Long newPackageId;
    @NotNull
    private PaymentMethod paymentMethod;
}