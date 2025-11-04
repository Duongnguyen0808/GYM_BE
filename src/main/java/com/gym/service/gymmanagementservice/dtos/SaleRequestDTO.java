package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class SaleRequestDTO {
    @Schema(description = "ID của hội viên (có thể null nếu là khách vãng lai)")
    private Long memberId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<SaleItemDTO> items;

    @Schema(description = "Hình thức thanh toán (Bắt buộc khi tạo hóa đơn POS, để trống khi tạo hóa đơn PENDING)")
    private PaymentMethod paymentMethod;
}