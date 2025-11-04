package com.gym.service.gymmanagementservice.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SaleItemDTO {
    @NotNull
    private Long productId;

    @NotNull
    @Min(value = 1, message = "Số lượng phải lớn hơn 0")
    private Integer quantity;
}