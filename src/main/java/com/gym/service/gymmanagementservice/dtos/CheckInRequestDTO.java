package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckInRequestDTO {
    @NotBlank(message = "Mã vạch là bắt buộc")
    @Schema(description = "Mã vạch/QR code được quét từ thẻ của hội viên")
    private String barcode;
}