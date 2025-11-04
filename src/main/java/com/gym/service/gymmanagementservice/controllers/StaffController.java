package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.AdminUpdateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.services.StaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff")
@Tag(name = "Staff Management API", description = "Các API để Admin quản lý nhân viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')") // Áp dụng quyền ADMIN cho tất cả các API trong controller này
public class StaffController {

    private final StaffService staffService;

    @GetMapping
    @Operation(summary = "Lấy danh sách tất cả người dùng (Chỉ Admin)")
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        return ResponseEntity.ok(staffService.getAllUsers());
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Admin cập nhật thông tin của một người dùng (Chỉ Admin)")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long userId, @Valid @RequestBody AdminUpdateUserRequestDTO request) {
        UserResponseDTO updatedUser = staffService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(updatedUser);
    }
}
