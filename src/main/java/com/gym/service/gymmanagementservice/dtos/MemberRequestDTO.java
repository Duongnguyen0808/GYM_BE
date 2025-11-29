package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Đối tượng yêu cầu để tạo hoặc cập nhật hội viên")
public class MemberRequestDTO {
    @NotBlank(message = "Họ tên là bắt buộc")
    private String fullName;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có 10 chữ số")
    private String phoneNumber;

    private String email;
    private LocalDate birthDate;
    private String address;
    
    @Schema(description = "Mật khẩu (tùy chọn, nếu không có sẽ dùng SĐT làm mật khẩu mặc định)")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;
}
