package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.SaleRequestDTO;
import com.gym.service.gymmanagementservice.models.Sale;
import com.gym.service.gymmanagementservice.services.SaleService;
import com.gym.service.gymmanagementservice.services.ReceiptService;
import io.swagger.v3.oas.annotations.Operation; // <-- IMPORT MỚI
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // <-- IMPORT MỚI
import io.swagger.v3.oas.annotations.tags.Tag; // <-- IMPORT MỚI
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/sales")
@Tag(name = "Sale (Product) API", description = "API quản lý bán sản phẩm")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SaleController {

    private final SaleService saleService;
    private final ReceiptService receiptService;

    @PostMapping("/pos")
    @Operation(summary = "Tạo hóa đơn bán tại quầy (POS) - (Đã thanh toán)")
    public ResponseEntity<Sale> createPosSale(@Valid @RequestBody SaleRequestDTO request) {
        Sale newSale = saleService.createPosSale(request);
        return new ResponseEntity<>(newSale, HttpStatus.CREATED);
    }

    @PostMapping("/pending")
    @Operation(summary = "Khởi tạo hóa đơn (chờ thanh toán online)")
    public ResponseEntity<Sale> createPendingSale(@Valid @RequestBody SaleRequestDTO request) {
        Sale pendingSale = saleService.createPendingSale(request);
        return new ResponseEntity<>(pendingSale, HttpStatus.CREATED);
    }

    @GetMapping("/{saleId}/receipt")
    @Operation(summary = "Tải PDF biên nhận bán hàng")
    public ResponseEntity<byte[]> downloadSaleReceipt(@PathVariable Long saleId) throws java.io.IOException {
        byte[] pdf = receiptService.generateSaleReceipt(saleId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=receipt-" + saleId + ".pdf")
                .body(pdf);
    }
}
