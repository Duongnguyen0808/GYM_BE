package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import com.gym.service.gymmanagementservice.services.AuthenticationService;
import com.gym.service.gymmanagementservice.services.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/member/subscriptions")
@Tag(name = "Member Subscription API", description = "API để hội viên tự mua/gia hạn gói tập")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("isAuthenticated()")
public class MemberSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final AuthenticationService authenticationService;
    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Hội viên tự đăng ký/mua gói tập cho chính mình")
    public ResponseEntity<SubscriptionResponseDTO> createForMe(@Valid @RequestBody SubscriptionRequestDTO request) {
        User current = authenticationService.getCurrentAuthenticatedUser();
        Member me = current.getMemberProfile();
        if (me == null) {
            Member created = Member.builder()
                    .fullName(current.getFullName())
                    .phoneNumber(current.getPhoneNumber())
                    .email(current.getEmail())
                    .barcode(current.getPhoneNumber())
                    .userAccount(current)
                    .build();
            memberRepository.save(created);
            current.setMemberProfile(created);
            current.setRole(Role.MEMBER);
            userRepository.save(current);
            me = created;
        }
        request.setMemberId(me.getId());
        SubscriptionResponseDTO res = subscriptionService.createSubscription(request);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PostMapping("/renew")
    @Operation(summary = "Hội viên tự gia hạn gói tập cho chính mình")
    public ResponseEntity<SubscriptionResponseDTO> renewForMe(@Valid @RequestBody SubscriptionRequestDTO request) {
        User current = authenticationService.getCurrentAuthenticatedUser();
        Member me = current.getMemberProfile();
        if (me == null) {
            Member created = Member.builder()
                    .fullName(current.getFullName())
                    .phoneNumber(current.getPhoneNumber())
                    .email(current.getEmail())
                    .barcode(current.getPhoneNumber())
                    .userAccount(current)
                    .build();
            memberRepository.save(created);
            current.setMemberProfile(created);
            current.setRole(Role.MEMBER);
            userRepository.save(current);
            me = created;
        }
        request.setMemberId(me.getId());
        SubscriptionResponseDTO res = subscriptionService.renewSubscription(request);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{subscriptionId}/upgrade/calculate")
    @Operation(summary = "Tính toán giá trị hoàn lại và số tiền phải bù khi nâng cấp")
    public ResponseEntity<com.gym.service.gymmanagementservice.dtos.UpgradeCalculationDTO> calculateUpgrade(
            @PathVariable Long subscriptionId,
            @RequestParam Long newPackageId) {
        User current = authenticationService.getCurrentAuthenticatedUser();
        Member me = current.getMemberProfile();
        if (me == null) {
            Member created = Member.builder()
                    .fullName(current.getFullName())
                    .phoneNumber(current.getPhoneNumber())
                    .email(current.getEmail())
                    .barcode(current.getPhoneNumber())
                    .userAccount(current)
                    .build();
            memberRepository.save(created);
            current.setMemberProfile(created);
            current.setRole(Role.MEMBER);
            userRepository.save(current);
            me = created;
        }
        // Kiểm tra subscription thuộc về member này
        com.gym.service.gymmanagementservice.models.MemberPackage subscription = 
            memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if (!subscription.getMember().getId().equals(me.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền xem thông tin gói tập này");
        }
        
        com.gym.service.gymmanagementservice.dtos.UpgradeCalculationDTO calculation = 
            subscriptionService.calculateUpgradePrice(subscriptionId, newPackageId);
        return ResponseEntity.ok(calculation);
    }

    @PatchMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Hội viên tự hủy gói tập cho chính mình")
    public ResponseEntity<Void> cancelForMe(@PathVariable Long subscriptionId) {
        User current = authenticationService.getCurrentAuthenticatedUser();
        Member me = current.getMemberProfile();
        if (me == null) {
            Member created = Member.builder()
                    .fullName(current.getFullName())
                    .phoneNumber(current.getPhoneNumber())
                    .email(current.getEmail())
                    .barcode(current.getPhoneNumber())
                    .userAccount(current)
                    .build();
            memberRepository.save(created);
            current.setMemberProfile(created);
            current.setRole(Role.MEMBER);
            userRepository.save(current);
            me = created;
        }
        // Kiểm tra subscription thuộc về member này
        com.gym.service.gymmanagementservice.models.MemberPackage subscription = 
            memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if (!subscription.getMember().getId().equals(me.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền hủy gói tập này");
        }
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{subscriptionId}/upgrade")
    @Operation(summary = "Hội viên tự nâng cấp gói tập cho chính mình")
    public ResponseEntity<SubscriptionResponseDTO> upgradeForMe(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody com.gym.service.gymmanagementservice.dtos.UpgradeRequestDTO request) {
        User current = authenticationService.getCurrentAuthenticatedUser();
        Member me = current.getMemberProfile();
        if (me == null) {
            Member created = Member.builder()
                    .fullName(current.getFullName())
                    .phoneNumber(current.getPhoneNumber())
                    .email(current.getEmail())
                    .barcode(current.getPhoneNumber())
                    .userAccount(current)
                    .build();
            memberRepository.save(created);
            current.setMemberProfile(created);
            current.setRole(Role.MEMBER);
            userRepository.save(current);
            me = created;
        }
        // Kiểm tra subscription thuộc về member này
        com.gym.service.gymmanagementservice.models.MemberPackage subscription = 
            memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if (!subscription.getMember().getId().equals(me.getId())) {
            throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền nâng cấp gói tập này");
        }
        SubscriptionResponseDTO res = subscriptionService.upgradeSubscription(
            subscriptionId, request.getNewPackageId(), request.getPaymentMethod());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/packages/{packageId}/available-time-slots")
    @Operation(summary = "Lấy danh sách các time slot khả dụng cho gói PT")
    public ResponseEntity<java.util.List<java.util.Map<String, Object>>> getAvailableTimeSlots(
            @PathVariable("packageId") Long packageId) {
        java.util.List<java.util.Map<String, Object>> slots = subscriptionService.getAvailableTimeSlots(packageId);
        return ResponseEntity.ok(slots);
    }
}