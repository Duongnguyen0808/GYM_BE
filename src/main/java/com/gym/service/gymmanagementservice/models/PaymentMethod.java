package com.gym.service.gymmanagementservice.models;

public enum PaymentMethod {
    CASH,           // Tiền mặt
    BANK_TRANSFER,  // Chuyển khoản (Dùng cho admin nhập thủ công)
    VN_PAY          // Thanh toán VNPay (Online)
}
