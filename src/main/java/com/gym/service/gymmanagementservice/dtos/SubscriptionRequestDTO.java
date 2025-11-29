package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng ký gói tập cho hội viên")
public class SubscriptionRequestDTO {
    @NotNull
    @Schema(description = "ID của hội viên")
    private Long memberId;

    @NotNull
    @Schema(description = "ID của gói tập (GymPackage)")
    private Long packageId;

    @NotNull(message = "Hình thức thanh toán là bắt buộc")
    private PaymentMethod paymentMethod;

    @Schema(description = "ID của PT được gán (chỉ dùng khi mua gói PT_SESSION)")
    private Long assignedPtId;

    @Schema(description = "Lịch tập theo tuần, dạng chuỗi CSV ví dụ: MON,WED,FRI")
    private String allowedWeekdays;

    @Schema(description = "Khung giờ đã chọn (chỉ dùng cho PT_SESSION): MORNING, AFTERNOON_1, AFTERNOON_2, EVENING")
    private TimeSlot timeSlot;
}
