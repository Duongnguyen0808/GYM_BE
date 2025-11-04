package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.MemberPackage;
import com.gym.service.gymmanagementservice.models.SubscriptionStatus;
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
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    private SubscriptionStatus status;

    public static SubscriptionResponseDTO fromMemberPackage(MemberPackage subscription) {
        return SubscriptionResponseDTO.builder()
                .subscriptionId(subscription.getId())
                .memberId(subscription.getMember().getId())
                .memberFullName(subscription.getMember().getFullName())
                .packageId(subscription.getGymPackage().getId())
                .packageName(subscription.getGymPackage().getName())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .build();
    }
}