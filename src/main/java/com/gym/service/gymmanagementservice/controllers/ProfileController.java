package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.ChangePasswordRequest;
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/profile")
@Tag(name = "User Profile API", description = "API cho người dùng tự quản lý thông tin cá nhân")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class ProfileController {

    private final AuthenticationService authenticationService;

    /**
     * API cho Endpoint GET /api/profile/me
     * Lấy thông tin cá nhân của người dùng đang đăng nhập.
     */
    @GetMapping("/me")
    @Operation(summary = "Lấy thông tin cá nhân của tôi")
    public ResponseEntity<UserResponseDTO> getMyProfile() {
        UserResponseDTO profile = authenticationService.getMyProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * API cho Endpoint PUT /api/profile/change-password
     * Người dùng tự thay đổi mật khẩu của mình.
     */
    @PutMapping("/change-password")
    @Operation(summary = "Tự thay đổi mật khẩu")
    public ResponseEntity<String> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        try {
            authenticationService.changePassword(request);
            return ResponseEntity.ok("Đổi mật khẩu thành công.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}