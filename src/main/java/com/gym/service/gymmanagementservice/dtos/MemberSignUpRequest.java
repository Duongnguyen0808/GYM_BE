package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Đối tượng yêu cầu để HỘI VIÊN tự đăng ký tài khoản")
public class MemberSignUpRequest {

    @Schema(description = "Họ và tên đầy đủ của hội viên", example = "Nguyễn Văn B")
    @NotBlank(message = "Họ và tên là bắt buộc")
    private String fullName;

    @Schema(description = "Số điện thoại của hội viên (dùng để đăng nhập)", example = "0912345678")
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 chữ số")
    private String phoneNumber;

    @Schema(description = "Mật khẩu (ít nhất 6 ký tự)", example = "password123")
    @NotBlank(message = "Mật khẩu là bắt buộc")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    // Các thông tin profile bổ sung
    @Schema(description = "Ngày sinh (YYYY-MM-DD)", example = "2000-01-30")
    private LocalDate birthDate;

    @Schema(description = "Địa chỉ", example = "123 Đường ABC, Quận 1, TP.HCM")
    private String address;
}