package com.gym.service.gymmanagementservice.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility class để generate BCrypt hash cho password
 * Chạy main method để lấy hash cho các password mẫu
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=== BCrypt Password Hashes ===");
        System.out.println("admin123: " + encoder.encode("admin123"));
        System.out.println("pt123: " + encoder.encode("pt123"));
        System.out.println("staff123: " + encoder.encode("staff123"));
        System.out.println("member123: " + encoder.encode("member123"));
    }
}





