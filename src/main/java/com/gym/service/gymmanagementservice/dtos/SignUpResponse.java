package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Đối tượng trả về sau khi đăng ký (chứa OTP mô phỏng)")
public class SignUpResponse {

    @Schema(description = "Thông báo kết quả")
    private String message;

    @Schema(description = "Mã OTP (Chỉ dùng cho demo/mô phỏng, sẽ bị xóa ở Production)")
    private String otpForDemo;
}