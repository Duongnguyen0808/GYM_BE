package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import com.gym.service.gymmanagementservice.services.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member")
@Tag(name = "Member-Facing API", description = "API dành cho hội viên (đã đăng nhập)")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('MEMBER')")
public class MemberProfileController {

    private final AuthenticationService authenticationService;
    private final SubscriptionService subscriptionService;

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

        if (memberProfile == null) {
            return ResponseEntity.status(404).body(null);
        }

        List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberProfile.getId());
        return ResponseEntity.ok(subscriptions);
    }
}