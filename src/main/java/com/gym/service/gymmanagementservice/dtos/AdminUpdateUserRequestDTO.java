package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu Admin dùng để cập nhật thông tin nhân viên")
public class AdminUpdateUserRequestDTO {

    @Schema(description = "Họ và tên đầy đủ", example = "Trần Thị B")
    @NotBlank(message = "Họ và tên là bắt buộc")
    private String fullName;

    @Schema(description = "Vai trò mới của người dùng", example = "PT")
    @NotNull(message = "Vai trò là bắt buộc")
    private Role role;

    @Schema(description = "Trạng thái khóa tài khoản (true = bị khóa, false = hoạt động)", example = "false")
    @NotNull(message = "Trạng thái khóa là bắt buộc")
    private boolean locked;
}