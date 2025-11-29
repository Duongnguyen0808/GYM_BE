package com.gym.service.gymmanagementservice.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TransferRequestDTO {
    @NotNull
    private Long toMemberId;
}