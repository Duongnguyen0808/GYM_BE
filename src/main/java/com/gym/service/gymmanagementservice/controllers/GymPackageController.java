package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.Promotion;
import com.gym.service.gymmanagementservice.services.PackageService;
import com.gym.service.gymmanagementservice.services.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Tag(name = "Package Management API", description = "Các API để quản lý gói tập")
@SecurityRequirement(name = "bearerAuth")
public class GymPackageController {

    private final PackageService packageService;
    private final PromotionService promotionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo một gói tập mới (Chỉ Admin)")
    @ApiResponse(responseCode = "201", description = "Tạo gói tập thành công")
    public ResponseEntity<PackageResponseDTO> createPackage(@Valid @RequestBody PackageRequestDTO request) {
        PackageResponseDTO newPackage = packageService.createPackage(request);
        return new ResponseEntity<>(newPackage, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy danh sách tất cả các gói tập (Admin, Staff)")
    public ResponseEntity<List<PackageResponseDTO>> getAllPackages() {
        List<PackageResponseDTO> packages = packageService.getAllPackages();
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Tìm kiếm/lọc gói tập")
    public ResponseEntity<List<PackageResponseDTO>> searchPackages(
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "minPrice", required = false) java.math.BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) java.math.BigDecimal maxPrice,
            @RequestParam(value = "durationDays", required = false) Integer durationDays,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "active", required = false) Boolean active
    ) {
        List<PackageResponseDTO> packages = packageService.searchPackages(q, minPrice, maxPrice, durationDays, type, active);
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy thông tin chi tiết một gói tập bằng ID (Admin, Staff)")
    public ResponseEntity<PackageResponseDTO> getPackageById(@PathVariable Long id) {
        PackageResponseDTO pkg = packageService.getPackageById(id);
        return ResponseEntity.ok(pkg);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật thông tin gói tập (Chỉ Admin)")
    public ResponseEntity<PackageResponseDTO> updatePackage(@PathVariable Long id, @Valid @RequestBody PackageRequestDTO request) {
        PackageResponseDTO updatedPackage = packageService.updatePackage(id, request);
        return ResponseEntity.ok(updatedPackage);
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kích hoạt hoặc vô hiệu hóa một gói tập (Chỉ Admin)")
    public ResponseEntity<Void> togglePackageStatus(@PathVariable Long id) {
        packageService.togglePackageStatus(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}/promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy thông tin promotion đang active cho gói tập")
    public ResponseEntity<Map<String, Object>> getPackagePromotion(@PathVariable Long id) {
        Optional<Promotion> promotion = promotionService.getActivePromotionForPackage(id);
        
        Map<String, Object> response = new HashMap<>();
        if (promotion.isPresent()) {
            Promotion promo = promotion.get();
            response.put("hasPromotion", true);
            response.put("name", promo.getName());
            response.put("discountPercent", promo.getDiscountPercent());
        } else {
            response.put("hasPromotion", false);
        }
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa gói tập (Chỉ Admin)")
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        packageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }
}
