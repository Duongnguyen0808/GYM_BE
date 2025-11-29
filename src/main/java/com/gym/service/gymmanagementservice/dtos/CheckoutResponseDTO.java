package com.gym.service.gymmanagementservice.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Đối tượng phản hồi sau khi thanh toán giỏ hàng")
public class CheckoutResponseDTO {
    
    @Schema(description = "URL thanh toán VNPay (chỉ có khi paymentMethod là VN_PAY)")
    private String paymentUrl;
    
    @Schema(description = "Thông báo kết quả")
    private String message;
    
    @Schema(description = "Trạng thái thành công")
    private Boolean success;
}


















