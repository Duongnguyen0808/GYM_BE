package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo/cập nhật lịch làm việc")
public class WorkScheduleRequestDTO {
    @Schema(description = "ID của nhân viên được xếp lịch", example = "2")
    @NotNull(message = "ID nhân viên là bắt buộc")
    private Long userId;

    @Schema(description = "Thời gian bắt đầu buổi/lớp", example = "2025-10-20T08:00")
    @NotNull(message = "Thời gian bắt đầu là bắt buộc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startTime;

    @Schema(description = "Thời gian kết thúc buổi/lớp", example = "2025-10-20T17:00")
    @NotNull(message = "Thời gian kết thúc là bắt buộc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endTime;

    @Schema(description = "Ghi chú cho ca làm", example = "Ca sáng chính")
    private String notes;
}
