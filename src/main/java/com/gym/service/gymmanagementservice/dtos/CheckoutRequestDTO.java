package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
@Schema(description = "Đối tượng yêu cầu thanh toán giỏ hàng (gói tập + sản phẩm)")
public class CheckoutRequestDTO {
    
    @Schema(description = "Danh sách gói tập cần thanh toán")
    @Valid
    private List<CheckoutPackageItemDTO> packages;
    
    @Schema(description = "Danh sách sản phẩm cần thanh toán")
    @Valid
    private List<CheckoutProductItemDTO> products;
    
    @NotNull(message = "Hình thức thanh toán là bắt buộc")
    @Schema(description = "Hình thức thanh toán")
    private PaymentMethod paymentMethod;
    
    @Data
    @Schema(description = "Item gói tập trong giỏ hàng")
    public static class CheckoutPackageItemDTO {
        @NotNull(message = "ID gói tập là bắt buộc")
        private Long packageId;
        
        @Schema(description = "Số lượng (mặc định là 1)")
        private Integer quantity = 1;
    }
    
    @Data
    @Schema(description = "Item sản phẩm trong giỏ hàng")
    public static class CheckoutProductItemDTO {
        @NotNull(message = "ID sản phẩm là bắt buộc")
        private Long productId;
        
        @NotNull(message = "Số lượng là bắt buộc")
        private Integer quantity;
    }
}

