package com.gym.service.gymmanagementservice.controllers;

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
        String payStatus = success ? "success" : (canceled ? "canceled" : "failed");
        return new RedirectView("/pos?pay=" + payStatus);
    }
}