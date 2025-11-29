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
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@Tag(name = "Subscription API", description = "API để đăng ký và xem các gói tập của hội viên")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final com.gym.service.gymmanagementservice.services.ReceiptService receiptService;

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

    @GetMapping("/search")
    @Operation(summary = "Tìm kiếm/lọc danh sách đăng ký")
    public ResponseEntity<List<SubscriptionResponseDTO>> searchSubscriptions(
            @RequestParam(value = "memberId", required = false) Long memberId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "packageType", required = false) String packageType,
            @RequestParam(value = "startFrom", required = false) java.time.OffsetDateTime startFrom,
            @RequestParam(value = "endTo", required = false) java.time.OffsetDateTime endTo,
            @RequestParam(value = "assignedPtId", required = false) Long assignedPtId
    ) {
        List<SubscriptionResponseDTO> result = subscriptionService.searchSubscriptions(memberId, status, packageType, startFrom, endTo, assignedPtId);
        return ResponseEntity.ok(result);
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

    @PostMapping("/{subscriptionId}/refund")
    @Operation(summary = "Hoàn tiền cho gói đã hủy")
    public ResponseEntity<Map<String, Object>> refundSubscription(@PathVariable Long subscriptionId,
                                                                  @RequestParam("paymentMethod") com.gym.service.gymmanagementservice.models.PaymentMethod paymentMethod) {
        java.math.BigDecimal amount = subscriptionService.refundSubscription(subscriptionId, paymentMethod);
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("refundAmount", amount);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/transaction/{transactionId}/receipt")
    @Operation(summary = "Tải PDF biên nhận giao dịch gói tập")
    public ResponseEntity<byte[]> downloadSubscriptionTransactionReceipt(@PathVariable Long transactionId) throws java.io.IOException {
        byte[] pdf = receiptService.generateTransactionReceipt(transactionId);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=transaction-" + transactionId + ".pdf")
                .body(pdf);
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

    @PatchMapping("/{subscriptionId}/upgrade")
    @Operation(summary = "Nâng cấp gói tập đang hoạt động")
    public ResponseEntity<SubscriptionResponseDTO> upgradeSubscription(@PathVariable Long subscriptionId,
                                                                       @Valid @RequestBody com.gym.service.gymmanagementservice.dtos.UpgradeRequestDTO request) {
        SubscriptionResponseDTO upgraded = subscriptionService.upgradeSubscription(subscriptionId, request.getNewPackageId(), request.getPaymentMethod());
        return ResponseEntity.ok(upgraded);
    }

    @PatchMapping("/{subscriptionId}/transfer")
    @Operation(summary = "Chuyển nhượng gói tập sang hội viên khác")
    public ResponseEntity<SubscriptionResponseDTO> transferSubscription(@PathVariable Long subscriptionId,
                                                                        @Valid @RequestBody com.gym.service.gymmanagementservice.dtos.TransferRequestDTO request) {
        SubscriptionResponseDTO transferred = subscriptionService.transferSubscription(subscriptionId, request.getToMemberId());
        return ResponseEntity.ok(transferred);
    }
}
