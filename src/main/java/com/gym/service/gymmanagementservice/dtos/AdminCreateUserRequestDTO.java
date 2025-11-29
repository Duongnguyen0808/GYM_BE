package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng Admin dùng để tạo tài khoản nhân viên mới")
public class AdminCreateUserRequestDTO {

    @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn A")
    @NotBlank(message = "Họ và tên là bắt buộc")
    private String fullName;

    @Schema(description = "Số điện thoại (dùng để đăng nhập)", example = "0987654321")
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 chữ số")
    private String phoneNumber;

    // Email đã bỏ, không cần nữa
    // private String email;

    @Schema(description = "Mật khẩu (ít nhất 6 ký tự)", example = "password123")
    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    @Schema(description = "Vai trò của tài khoản mới", example = "STAFF")
    @NotNull(message = "Vai trò là bắt buộc")
    private Role role;
}
