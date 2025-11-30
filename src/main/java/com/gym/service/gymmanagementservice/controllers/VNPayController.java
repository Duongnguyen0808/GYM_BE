package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import com.gym.service.gymmanagementservice.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vnpay")
@Tag(name = "VNPay Payment API", description = "API tích hợp cổng thanh toán VNPay")
public class VNPayController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;

    // Endpoint này được gọi từ Frontend để bắt đầu thanh toán GÓI TẬP
    @PostMapping("/create-subscription-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // Chỉ nhân viên/admin mới tạo được yêu cầu thanh toán
    @Operation(summary = "Tạo yêu cầu thanh toán VNPay cho gói tập")
    public ResponseEntity<String> createSubscriptionPayment(HttpServletRequest request, @Valid @RequestBody SubscriptionRequestDTO subscriptionRequest) {
        String paymentUrl = paymentService.createSubscriptionPaymentUrl(
                request,
                subscriptionRequest.getMemberId(),
                subscriptionRequest.getPackageId()
        );
        return ResponseEntity.ok(paymentUrl);
    }

    // MỚI: Endpoint này được gọi để bắt đầu thanh toán HÓA ĐƠN BÁN HÀNG
    @PostMapping("/create-sale-payment/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')") // Hoặc có thể cho cả Member tự thanh toán online
    @Operation(summary = "Tạo yêu cầu thanh toán VNPay cho hóa đơn bán hàng")
    public ResponseEntity<String> createSalePayment(
            HttpServletRequest request,
            @Parameter(description = "ID của hóa đơn bán hàng (Sale) cần thanh toán") @PathVariable Long saleId) {

        // Gọi service để tạo Transaction PENDING, cập nhật Sale PENDING_PAYMENT và lấy URL VNPay
        String paymentUrl = paymentService.createSalePaymentUrl(request, saleId);
        return ResponseEntity.ok(paymentUrl);
    }

    // Endpoint này VNPay sẽ gọi ngầm (IPN) - CÔNG KHAI
    @GetMapping("/ipn")
    @Operation(summary = "Endpoint nhận Instant Payment Notification (IPN) từ VNPay (Public)")
    public ResponseEntity<String> handleVNPayIPN(@RequestParam Map<String, String[]> params) {
        boolean success = paymentService.processVNPayIPN(params);
        if (success) {
            return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        } else {
            return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Confirm Failed\"}");
        }
    }

    // Endpoint này trình duyệt của người dùng sẽ được chuyển về sau khi thanh toán - CÔNG KHAI
    @GetMapping("/return")
    @Operation(summary = "Endpoint xử lý khi người dùng được chuyển về từ VNPay (Public)")
    public RedirectView handleVNPayReturn(@RequestParam Map<String, String[]> params) {
        String responseCode = params.get("vnp_ResponseCode")[0];
        boolean success = "00".equals(responseCode);
        boolean canceled = "24".equals(responseCode);
        String transactionIdStr = params.containsKey("vnp_TxnRef") && params.get("vnp_TxnRef").length > 0 
            ? params.get("vnp_TxnRef")[0] : "";
        
        // Kiểm tra transaction để xác định nguồn thanh toán (admin web vs mobile app)
        // Dựa vào role của user tạo transaction
        boolean isFromAdminWeb = false;
        if (!transactionIdStr.isEmpty()) {
            try {
                Long transactionId = Long.parseLong(transactionIdStr);
                Transaction transaction = transactionRepository.findById(transactionId).orElse(null);
                if (transaction != null && transaction.getCreatedBy() != null) {
                    Role userRole = transaction.getCreatedBy().getRole();
                    // Nếu user tạo transaction là ADMIN hoặc STAFF, đó là thanh toán từ Admin web
                    isFromAdminWeb = (userRole == Role.ADMIN || userRole == Role.STAFF);
                }
            } catch (Exception e) {
                // Nếu không parse được transactionId hoặc không tìm thấy transaction, 
                // mặc định là mobile app
            }
        }
        
        // Nếu thanh toán từ Admin web, redirect về trang thống kê
        if (isFromAdminWeb) {
            return new RedirectView("/admin/reports/sales");
        }
        
        // Nếu thanh toán từ mobile app, redirect về deep link
        String status = success ? "success" : (canceled ? "canceled" : "failed");
        String deepLink = "gymapp://payment-result?status=" + status;
        if (!transactionIdStr.isEmpty()) {
            deepLink += "&transactionId=" + transactionIdStr;
        }
        
        // Redirect to deep link - this will open the mobile app
        return new RedirectView(deepLink);
    }
}