package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.dtos.TransactionReportDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInLogResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import com.gym.service.gymmanagementservice.services.ReportService;
import com.gym.service.gymmanagementservice.services.SubscriptionService;
import com.gym.service.gymmanagementservice.services.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import com.gym.service.gymmanagementservice.services.MemberService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
@Tag(name = "Member-Facing API", description = "API dành cho hội viên (đã đăng nhập)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()") // Cho phép tất cả user đã đăng nhập, service sẽ tự tạo member profile
public class MemberProfileController {

    private final AuthenticationService authenticationService;
    private final SubscriptionService subscriptionService;
    private final ReportService reportService;
    private final CheckInService checkInService;
    private final MemberService memberService;
    private final MemberRepository memberRepository;
    private final UserRepository userRepository;

    /**
     * Lấy hồ sơ (profile) gym của hội viên đang đăng nhập
     */
    @GetMapping("/profile")
    @Operation(summary = "Lấy hồ sơ gym của tôi (Hội viên)")
    public ResponseEntity<MemberResponseDTO> getMyMemberProfile() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        if (memberProfile == null) {
            return ResponseEntity.status(404).body(null); // Hoặc ném lỗi
        }

        return ResponseEntity.ok(MemberResponseDTO.fromMember(memberProfile));
    }

    /**
     * Lấy danh sách tất cả các gói tập của hội viên đang đăng nhập
     */
    @GetMapping("/packages")
    @Operation(summary = "Lấy danh sách gói tập của tôi (Hội viên)")
    public ResponseEntity<List<SubscriptionResponseDTO>> getMyPackages() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        // Tự động tạo member profile nếu chưa có (giống MemberSubscriptionController)
        if (memberProfile == null) {
            memberProfile = Member.builder()
                    .fullName(currentUser.getFullName())
                    .phoneNumber(currentUser.getPhoneNumber())
                    .email(currentUser.getEmail())
                    .barcode(currentUser.getPhoneNumber())
                    .userAccount(currentUser)
                    .build();
            memberProfile = memberRepository.save(memberProfile);
            currentUser.setMemberProfile(memberProfile);
            currentUser.setRole(Role.MEMBER);
            userRepository.save(currentUser);
        }

        List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberProfile.getId());
        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Lấy lịch sử giao dịch (thanh toán) của hội viên đang đăng nhập
     */
    @GetMapping("/transactions")
    @Operation(summary = "Lấy lịch sử giao dịch của tôi (Hội viên)")
    public ResponseEntity<List<TransactionReportDTO>> getMyTransactions() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        if (memberProfile == null) {
            return ResponseEntity.status(404).body(null);
        }

        List<TransactionReportDTO> transactions = reportService.getMemberTimeline(memberProfile.getId());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Lấy lịch sử check-in/check-out của hội viên đang đăng nhập
     */
    @GetMapping("/check-in-logs")
    @Operation(summary = "Lấy lịch sử check-in/check-out của tôi (Hội viên)")
    public ResponseEntity<List<CheckInLogResponseDTO>> getMyCheckInLogs() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        if (memberProfile == null) {
            return ResponseEntity.status(404).body(null);
        }

        org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(0, 200);
        java.util.List<com.gym.service.gymmanagementservice.models.CheckInLog> logs = 
                checkInService.getCheckInLogs(memberProfile.getId(), null, null, null, pageable);
        
        List<CheckInLogResponseDTO> logDTOs = logs.stream()
                .map(CheckInLogResponseDTO::fromCheckInLog)
                .collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(logDTOs);
    }

    /**
     * Lấy session check-in đang hoạt động của hội viên đang đăng nhập
     */
    @GetMapping("/active-check-in")
    @Operation(summary = "Lấy session check-in đang hoạt động (Hội viên)")
    public ResponseEntity<CheckInLogResponseDTO> getMyActiveCheckIn() {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        if (memberProfile == null) {
            return ResponseEntity.status(404).body(null);
        }

        java.util.Optional<com.gym.service.gymmanagementservice.models.CheckInLog> activeLog = 
                checkInService.getActiveCheckInByMemberId(memberProfile.getId());
        
        if (activeLog.isPresent()) {
            return ResponseEntity.ok(CheckInLogResponseDTO.fromCheckInLog(activeLog.get()));
        }
        
        return ResponseEntity.ok(null);
    }

    /**
     * Cập nhật hồ sơ của chính mình (hội viên)
     */
    @PutMapping("/profile")
    @Operation(summary = "Cập nhật hồ sơ của tôi (Hội viên)")
    public ResponseEntity<MemberResponseDTO> updateMyProfile(@Valid @RequestBody com.gym.service.gymmanagementservice.dtos.MemberRequestDTO request) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member memberProfile = currentUser.getMemberProfile();

        // Tự động tạo member profile nếu chưa có
        if (memberProfile == null) {
            memberProfile = Member.builder()
                    .fullName(currentUser.getFullName())
                    .phoneNumber(currentUser.getPhoneNumber())
                    .email(currentUser.getEmail())
                    .barcode(currentUser.getPhoneNumber())
                    .userAccount(currentUser)
                    .build();
            memberProfile = memberRepository.save(memberProfile);
            currentUser.setMemberProfile(memberProfile);
            currentUser.setRole(Role.MEMBER);
            userRepository.save(currentUser);
        }

        // Update profile của chính mình (không cần kiểm tra ID vì đã lấy từ currentUser)
        MemberResponseDTO updated = memberService.updateMember(memberProfile.getId(), request);
        return ResponseEntity.ok(updated);
    }
}