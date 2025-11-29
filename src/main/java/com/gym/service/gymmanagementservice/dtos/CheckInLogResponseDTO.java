package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.CheckInLog;
import com.gym.service.gymmanagementservice.models.CheckInStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về thông tin lịch sử check-in/check-out")
public class CheckInLogResponseDTO {
    @Schema(description = "ID của log")
    private Long id;
    
    @Schema(description = "ID hội viên")
    private Long memberId;
    
    @Schema(description = "Tên hội viên")
    private String memberFullName;
    
    @Schema(description = "Số điện thoại hội viên")
    private String memberPhone;
    
    @Schema(description = "ID gói tập (nếu có)")
    private Long packageId;
    
    @Schema(description = "Tên gói tập (nếu có)")
    private String packageName;
    
    @Schema(description = "Thời gian check-in")
    private OffsetDateTime checkInTime;
    
    @Schema(description = "Thời gian check-out (nếu đã check-out)")
    private OffsetDateTime checkOutTime;
    
    @Schema(description = "Thời gian tập (giây)")
    private Long sessionDurationSeconds;
    
    @Schema(description = "Trạng thái check-in")
    private CheckInStatus status;
    
    @Schema(description = "Thông báo")
    private String message;
    
    @Schema(description = "Đã check-out chưa")
    private Boolean isCheckedOut;
    
    @Schema(description = "Số buổi còn lại tại thời điểm check-in (nếu là gói theo buổi)")
    private Integer remainingSessionsAtCheckIn;
    
    @Schema(description = "Loại gói tập")
    private com.gym.service.gymmanagementservice.models.PackageType packageType;
    
    public static CheckInLogResponseDTO fromCheckInLog(CheckInLog log) {
        Integer remainingSessions = null;
        com.gym.service.gymmanagementservice.models.PackageType pkgType = null;
        
        // Extract remaining sessions from message if available
        if (log.getMessage() != null && log.getMemberPackage() != null) {
            pkgType = log.getMemberPackage().getGymPackage() != null 
                    ? log.getMemberPackage().getGymPackage().getPackageType() : null;
            
            // Try to parse from message (format: "Đang tập PT — Còn lại X buổi." or "Đang tập — Còn lại X lượt.")
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("Còn lại (\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(log.getMessage());
            if (matcher.find()) {
                try {
                    remainingSessions = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            
            // If not found in message, get from current subscription
            if (remainingSessions == null && log.getMemberPackage() != null) {
                remainingSessions = log.getMemberPackage().getRemainingSessions();
            }
        }
        
        return CheckInLogResponseDTO.builder()
                .id(log.getId())
                .memberId(log.getMember() != null ? log.getMember().getId() : null)
                .memberFullName(log.getMember() != null ? log.getMember().getFullName() : null)
                .memberPhone(log.getMember() != null ? log.getMember().getPhoneNumber() : null)
                .packageId(log.getMemberPackage() != null ? log.getMemberPackage().getId() : null)
                .packageName(log.getMemberPackage() != null && log.getMemberPackage().getGymPackage() != null 
                        ? log.getMemberPackage().getGymPackage().getName() : null)
                .checkInTime(log.getCheckInTime())
                .checkOutTime(log.getCheckOutTime())
                .sessionDurationSeconds(log.getSessionDurationSeconds())
                .status(log.getStatus())
                .message(log.getMessage())
                .isCheckedOut(log.getCheckOutTime() != null)
                .remainingSessionsAtCheckIn(remainingSessions)
                .packageType(pkgType)
                .build();
    }
}

