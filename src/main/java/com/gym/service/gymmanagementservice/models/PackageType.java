package com.gym.service.gymmanagementservice.models;

public enum PackageType {
    GYM_ACCESS("Gói thời hạn"),   // Vào cửa theo số ngày, không giới hạn lượt
    PT_SESSION("Gói PT"),         // Gói tập PT, tính theo số buổi
    PER_VISIT("Gói theo lượt");   // Vào cửa theo lượt
    
    private final String displayName;
    
    PackageType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}
