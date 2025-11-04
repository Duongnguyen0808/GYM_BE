package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication API", description = "Các API về Đăng ký, Đăng nhập và Xác thực")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @Operation(summary = "Đăng ký tài khoản mới bằng SĐT", description = "API cho phép người dùng mới đăng ký. Tài khoản sẽ cần xác thực qua OTP.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công, chờ xác thực OTP. (Trả về OTP mô phỏng)"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc SĐT/Email đã tồn tại")
    })
    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signup(@Valid @RequestBody SignUpRequest request) {
        SignUpResponse response = authenticationService.signup(request);
        return ResponseEntity.ok(response);
    }

    // --- HÀM MỚI: Đăng ký cho Hội viên ---
    @Operation(summary = "Đăng ký tài khoản HỘI VIÊN (MEMBER) mới", description = "Dành cho hội viên tự đăng ký trên app di động.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công, chờ xác thực OTP."),
            @ApiResponse(responseCode = "400", description = "SĐT đã tồn tại")
    })
    @PostMapping("/member-signup")
    public ResponseEntity<SignUpResponse> memberSignup(@Valid @RequestBody MemberSignUpRequest request) {
        SignUpResponse response = authenticationService.memberSignup(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Đăng nhập vào hệ thống bằng SĐT", description = "Cung cấp SĐT và mật khẩu để nhận về JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về token"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Sai SĐT/mật khẩu hoặc tài khoản chưa được kích hoạt/bị khóa")
    })
    @PostMapping("/signin")
    public ResponseEntity<JwtAuthenticationResponse> signin(@Valid @RequestBody SignInRequest request) {
        JwtAuthenticationResponse response = authenticationService.signin(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Xác thực tài khoản bằng OTP", description = "Người dùng gửi SĐT và OTP để kích hoạt tài khoản.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Xác thực thành công"),
            @ApiResponse(responseCode = "400", description = "OTP không hợp lệ, hết hạn hoặc SĐT không tồn tại")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyAccount(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            String message = authenticationService.verifyOtp(request);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}