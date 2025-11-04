package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
@Schema(description = "Đối tượng trả về chứa thông tin công khai của người dùng")
public class UserResponseDTO {

    @Schema(description = "ID duy nhất của người dùng", example = "1")
    private Long id;

    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn A")
    private String fullName;

    @Schema(description = "Địa chỉ email", example = "nguyenvana@example.com")
    private String email;

    @Schema(description = "Số điện thoại", example = "0987654321")
    private String phoneNumber;

    @Schema(description = "Vai trò của người dùng trong hệ thống", example = "PATIENT")
    private Role role;

    @Schema(description = "Trạng thái kích hoạt tài khoản (true = đã kích hoạt)", example = "true")
    private boolean enabled;

    @Schema(description = "Trạng thái khóa tài khoản (true = đã bị khóa)", example = "false")
    private boolean locked;

    @Schema(description = "Thời điểm tài khoản được tạo", example = "2025-09-10T10:30:00+07:00")
    private OffsetDateTime createdAt;

    public static UserResponseDTO fromUser(User user) {
        return UserResponseDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
