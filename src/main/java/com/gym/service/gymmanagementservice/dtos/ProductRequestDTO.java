package com.gym.service.gymmanagementservice.dtos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductRequestDTO {
    @NotBlank(message = "Tên sản phẩm là bắt buộc")
    private String name;

    @NotNull(message = "Giá là bắt buộc")
    @DecimalMin(value = "0.0", message = "Giá phải là số không âm")
    private BigDecimal price;

    @NotNull(message = "Số lượng tồn kho là bắt buộc")
    @Min(value = 0, message = "Số lượng tồn kho phải là số không âm")
    private Integer stockQuantity;

    @JsonProperty("hinh_anh")
    private String imageUrl;
}