package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.MemberPackage;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.gym.service.gymmanagementservice.models.SubscriptionStatus;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về chứa thông tin chi tiết về một lần đăng ký gói tập")
public class SubscriptionResponseDTO {
    private Long subscriptionId;
    private Long memberId;
    private String memberFullName;
    private Long packageId;
    private String packageName;
    private PackageType packageType;
    private java.math.BigDecimal price;
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private SubscriptionStatus status;
    private Integer remainingSessions;
    private String ptFullName;
    private TimeSlot timeSlot; // Khung giờ đã chọn (chỉ dùng cho PT_SESSION)
    private String allowedWeekdays; // Các thứ trong tuần cho phép tập (chỉ dùng cho PT_SESSION)
    private Integer packageDurationDays; // Tổng số ngày của gói
    private Integer packageTotalSessions; // Tổng số buổi của gói
    private String imageUrl; // Hình ảnh gói tập

    public static SubscriptionResponseDTO fromMemberPackage(MemberPackage subscription) {
        return SubscriptionResponseDTO.builder()
                .subscriptionId(subscription.getId())
                .memberId(subscription.getMember().getId())
                .memberFullName(subscription.getMember().getFullName())
                .packageId(subscription.getGymPackage().getId())
                .packageName(subscription.getGymPackage().getName())
                .packageType(subscription.getGymPackage().getPackageType())
                .price(subscription.getGymPackage().getPrice())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .remainingSessions(subscription.getRemainingSessions())
                .ptFullName(subscription.getAssignedPt() != null ? subscription.getAssignedPt().getFullName() : null)
                .timeSlot(subscription.getTimeSlot())
                .allowedWeekdays(subscription.getAllowedWeekdays())
                .packageDurationDays(subscription.getGymPackage().getDurationDays())
                .packageTotalSessions(subscription.getGymPackage().getNumberOfSessions())
                .imageUrl(subscription.getGymPackage().getHinhAnh())
                .build();
    }
}