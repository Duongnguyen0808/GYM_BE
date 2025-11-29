package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.CheckoutRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckoutResponseDTO;
import com.gym.service.gymmanagementservice.services.MemberCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/checkout")
@Tag(name = "Member Checkout API", description = "API để hội viên thanh toán giỏ hàng (gói tập + sản phẩm)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()") // Cho phép tất cả user đã đăng nhập, service sẽ tự tạo member profile
public class MemberCheckoutController {

    private final MemberCheckoutService checkoutService;

    @PostMapping
    @Operation(summary = "Thanh toán giỏ hàng (hỗ trợ VNPay và các phương thức khác)")
    public ResponseEntity<CheckoutResponseDTO> checkout(
            HttpServletRequest request,
            @Valid @RequestBody CheckoutRequestDTO checkoutRequest) {
        CheckoutResponseDTO response = checkoutService.processCheckout(request, checkoutRequest);
        return ResponseEntity.ok(response);
    }
}

