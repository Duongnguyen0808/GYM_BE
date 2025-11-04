package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Đối tượng yêu cầu để xác thực OTP")
public class VerifyOtpRequest {

    @Schema(description = "Số điện thoại cần xác thực", example = "0987654321")
    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 chữ số")
    private String phoneNumber;

    @Schema(description = "Mã OTP 6 số", example = "123456")
    @NotBlank(message = "OTP là bắt buộc")
    @Size(min = 6, max = 6, message = "OTP phải có 6 chữ số")
    private String otp;
}