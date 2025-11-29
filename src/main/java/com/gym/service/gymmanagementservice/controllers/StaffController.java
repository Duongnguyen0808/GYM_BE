package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.AdminCreateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.AdminUpdateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
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
    private final AuthenticationService authenticationService;

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

    @PostMapping
    @Operation(summary = "Admin tạo tài khoản nhân viên/PT mới (Chỉ Admin)")
    public ResponseEntity<String> createStaff(@Valid @RequestBody AdminCreateUserRequestDTO request) {
        authenticationService.createStaffAccount(request);
        return ResponseEntity.ok("Tạo tài khoản nhân viên thành công!");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Admin xóa tài khoản nhân viên/PT (Chỉ Admin)")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        staffService.deleteUserByAdmin(userId);
        return ResponseEntity.noContent().build();
    }
}
