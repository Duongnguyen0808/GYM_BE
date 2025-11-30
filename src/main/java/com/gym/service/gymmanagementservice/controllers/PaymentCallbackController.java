package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.Transaction;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import com.gym.service.gymmanagementservice.services.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/vnpay")
public class PaymentCallbackController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/vnpay_ipn")
    @Operation(summary = "VNPay IPN callback (Public)")
    public ResponseEntity<String> ipn(@RequestParam Map<String, String> params) {
        Map<String, String[]> converted = new HashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            converted.put(e.getKey(), new String[]{e.getValue()});
        }
        boolean ok = paymentService.processVNPayIPN(converted);
        if (ok) return ResponseEntity.ok("{\"RspCode\":\"00\",\"Message\":\"Confirm Success\"}");
        return ResponseEntity.ok("{\"RspCode\":\"97\",\"Message\":\"Confirm Failed\"}");
    }

    @GetMapping("/vnpay_return")
    @Operation(summary = "VNPay browser return (Public)")
    public RedirectView ret(@RequestParam Map<String, String> params) {
        String code = params.getOrDefault("vnp_ResponseCode", "99");
        boolean success = "00".equals(code);
        boolean canceled = "24".equals(code);
        String transactionIdStr = params.getOrDefault("vnp_TxnRef", "");
        
        // Xử lý cập nhật transaction status ngay khi user quay về (fallback nếu IPN chưa được gọi)
        if (!transactionIdStr.isEmpty()) {
            try {
                // Convert params để xử lý như IPN
                Map<String, String[]> converted = new HashMap<>();
                for (Map.Entry<String, String> e : params.entrySet()) {
                    converted.put(e.getKey(), new String[]{e.getValue()});
                }
                // Xử lý cập nhật transaction status
                paymentService.processVNPayIPN(converted);
            } catch (Exception e) {
                // Log lỗi nhưng vẫn redirect
                // IPN sẽ được gọi lại sau nếu có lỗi
            }
        }
        
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