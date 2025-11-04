package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PackageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo hoặc cập nhật gói tập")
public class PackageRequestDTO {

    @Schema(description = "Tên gói tập", example = "Gói tập 1 tháng")
    @NotBlank(message = "Tên gói tập là bắt buộc")
    private String name;

    @Schema(description = "Mô tả chi tiết về gói tập", example = "Gói tập không giới hạn cho mọi khung giờ")
    private String description;

    @Schema(description = "Giá của gói tập", example = "500000.00")
    @NotNull(message = "Giá là bắt buộc")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Schema(description = "Loại gói tập: GYM_ACCESS, PT_SESSION, hoặc PER_VISIT")
    @NotNull(message = "Loại gói tập là bắt buộc")
    private PackageType packageType;

    @Schema(description = "Thời hạn của gói (số ngày). Bắt buộc cho GYM_ACCESS và PER_VISIT", example = "30")
    @Min(value = 1, message = "Thời hạn phải ít nhất là 1 ngày")
    private Integer durationDays;

    @Schema(description = "Số buổi (hoặc số lượt). Bắt buộc cho PT_SESSION và PER_VISIT", example = "10")
    @Min(value = 1, message = "Số buổi/lượt phải ít nhất là 1")
    private Integer numberOfSessions;

    @Schema(description = "Giờ check-in sớm nhất (định dạng HH:mm)", example = "09:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTimeLimit;

    @Schema(description = "Giờ check-in trễ nhất (định dạng HH:mm)", example = "16:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTimeLimit;
}