package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đóng băng gói tập")
public class FreezeRequestDTO {
    @NotNull
    @Min(value = 1, message = "Số ngày đóng băng phải lớn hơn 0")
    @Schema(description = "Số ngày cần đóng băng gói tập", example = "30")
    private Integer freezeDays;
}