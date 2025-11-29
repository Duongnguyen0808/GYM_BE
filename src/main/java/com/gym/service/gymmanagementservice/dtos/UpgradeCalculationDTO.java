package com.gym.service.gymmanagementservice.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpgradeCalculationDTO {
    private BigDecimal currentPackagePrice; // Giá gói hiện tại
    private BigDecimal newPackagePrice; // Giá gói mới
    private BigDecimal refundValue; // Giá trị hoàn lại
    private BigDecimal amountToPay; // Số tiền phải bù thêm
    private String calculationDetails; // Chi tiết tính toán (optional)
}


