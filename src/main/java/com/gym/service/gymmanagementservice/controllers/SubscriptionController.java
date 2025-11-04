package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.FreezeRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscription API", description = "API để đăng ký và xem các gói tập của hội viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @Operation(summary = "Đăng ký một gói tập cho một hội viên")
    public ResponseEntity<SubscriptionResponseDTO> createSubscription(@Valid @RequestBody SubscriptionRequestDTO request) {
        SubscriptionResponseDTO newSubscription = subscriptionService.createSubscription(request);
        return new ResponseEntity<>(newSubscription, HttpStatus.CREATED);
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Lấy danh sách tất cả các lần đăng ký của một hội viên")
    public ResponseEntity<List<SubscriptionResponseDTO>> getSubscriptionsByMember(@PathVariable Long memberId) {
        List<SubscriptionResponseDTO> subscriptions = subscriptionService.getSubscriptionsByMemberId(memberId);
        return ResponseEntity.ok(subscriptions);
    }

    @PostMapping("/renew")
    @Operation(summary = "Gia hạn gói tập cho hội viên")
    public ResponseEntity<SubscriptionResponseDTO> renewSubscription(@Valid @RequestBody SubscriptionRequestDTO request) {
        SubscriptionResponseDTO renewedSubscription = subscriptionService.renewSubscription(request);
        return ResponseEntity.ok(renewedSubscription);
    }

    @PatchMapping("/{subscriptionId}/cancel")
    @Operation(summary = "Hủy một gói đăng ký đang hoạt động")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.cancelSubscription(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{subscriptionId}/freeze")
    @Operation(summary = "Đóng băng một gói đăng ký đang hoạt động")
    public ResponseEntity<SubscriptionResponseDTO> freezeSubscription(@PathVariable Long subscriptionId, @Valid @RequestBody FreezeRequestDTO request) {
        SubscriptionResponseDTO frozenSubscription = subscriptionService.freezeSubscription(subscriptionId, request);
        return ResponseEntity.ok(frozenSubscription);
    }

    @PatchMapping("/{subscriptionId}/unfreeze")
    @Operation(summary = "Kích hoạt lại một gói đăng ký đang bị đóng băng")
    public ResponseEntity<SubscriptionResponseDTO> unfreezeSubscription(@PathVariable Long subscriptionId) {
        SubscriptionResponseDTO unfrozenSubscription = subscriptionService.unfreezeSubscription(subscriptionId);
        return ResponseEntity.ok(unfrozenSubscription);
    }
}