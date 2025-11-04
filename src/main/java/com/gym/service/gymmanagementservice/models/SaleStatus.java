package com.gym.service.gymmanagementservice.models;

public enum SaleStatus {
    PENDING_PAYMENT, // Chờ thanh toán (đặc biệt khi mua online)
    PAID,            // Đã thanh toán
    CANCELLED,       // Đã hủy
    PAYMENT_FAILED   // Thanh toán thất bại
}
