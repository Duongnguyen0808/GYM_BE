package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo/cập nhật lịch làm việc")
public class WorkScheduleRequestDTO {
    @Schema(description = "ID của nhân viên được xếp lịch", example = "2")
    @NotNull(message = "ID nhân viên là bắt buộc")
    private Long userId;

    @Schema(description = "Thời gian bắt đầu ca làm", example = "2025-10-20T08:00:00+07:00")
    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    private OffsetDateTime startTime;

    @Schema(description = "Thời gian kết thúc ca làm", example = "2025-10-20T17:00:00+07:00")
    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    private OffsetDateTime endTime;

    @Schema(description = "Ghi chú cho ca làm", example = "Ca sáng chính")
    private String notes;
}
