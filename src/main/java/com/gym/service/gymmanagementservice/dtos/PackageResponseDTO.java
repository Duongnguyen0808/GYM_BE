package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về chứa thông tin chi tiết của một gói tập")
public class PackageResponseDTO {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal originalPrice; // Giá gốc (nếu có promotion)
    private BigDecimal discountedPrice; // Giá đã giảm (nếu có promotion)
    private PackageType packageType;
    private Integer durationDays;
    private Integer durationMonths; // Thời hạn tính theo tháng (cho PT_SESSION)
    private Integer numberOfSessions;
    private Integer sessionsPerWeek;
    private Boolean unlimited;
    private LocalTime startTimeLimit;
    private LocalTime endTimeLimit;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Integer activeMemberCount;

    @JsonProperty("hinh_anh")
    private String imageUrl;

    private Long assignedPtId; // ID của PT được gán cho gói (chỉ dùng cho PT_SESSION)
    private String assignedPtName; // Tên của PT được gán
    private String allowedWeekdays; // Các thứ trong tuần cho phép tập, định dạng CSV: "MON,WED,FRI" (chỉ dùng cho PT_SESSION)

    public static PackageResponseDTO fromPackage(GymPackage pkg) {
        return PackageResponseDTO.builder()
                .id(pkg.getId())
                .name(pkg.getName())
                .description(pkg.getDescription())
                .price(pkg.getPrice())
                .packageType(pkg.getPackageType())
                .durationDays(pkg.getDurationDays())
                .durationMonths(pkg.getDurationMonths())
                .numberOfSessions(pkg.getNumberOfSessions())
                .sessionsPerWeek(pkg.getSessionsPerWeek())
                .unlimited(pkg.getUnlimited())
                .startTimeLimit(pkg.getStartTimeLimit())
                .endTimeLimit(pkg.getEndTimeLimit())
                .isActive(pkg.isActive())
                .createdAt(pkg.getCreatedAt())
                .updatedAt(pkg.getUpdatedAt())
                .imageUrl(pkg.getHinhAnh())
                .assignedPtId(pkg.getAssignedPt() != null ? pkg.getAssignedPt().getId() : null)
                .assignedPtName(pkg.getAssignedPt() != null ? pkg.getAssignedPt().getFullName() : null)
                .allowedWeekdays(pkg.getAllowedWeekdays())
                .activeMemberCount(null)
                .build();
    }
}
