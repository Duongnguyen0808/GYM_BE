package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để đăng nhập")
public class SignInRequest {

    @Schema(description = "Số điện thoại đã đăng ký", example = "0987654321")
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 chữ số")
    private String phoneNumber;

    @Schema(description = "Mật khẩu đã đăng ký", example = "password123")
    @NotBlank(message = "Mật khẩu là bắt buộc")
    private String password;
}

