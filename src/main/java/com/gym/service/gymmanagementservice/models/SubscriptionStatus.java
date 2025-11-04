package com.gym.service.gymmanagementservice.models;

public enum SubscriptionStatus {
    ACTIVE,     // Đang hoạt động
    EXPIRED,    // Đã hết hạn
    FROZEN,     // Đang đóng băng
    CANCELLED,  // Đã hủy
    PENDING     // Chờ thanh toán/kích hoạt
}
