package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.services.PackageService;
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

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Tag(name = "Package Management API", description = "Các API để quản lý gói tập")
@SecurityRequirement(name = "bearerAuth")
public class GymPackageController {

    private final PackageService packageService;

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
}
