package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.CheckInStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về kết quả sau khi check-in")
public class CheckInResponseDTO {
    @Schema(description = "Trạng thái của lần check-in")
    private CheckInStatus status;

    @Schema(description = "Thông báo kết quả cho nhân viên")
    private String message;

    private String memberFullName;
    private String packageName;
    private OffsetDateTime packageEndDate;
}