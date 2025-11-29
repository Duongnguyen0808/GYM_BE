package com.gym.service.gymmanagementservice.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.security.SecureRandom;

@Service
public class DailyQrService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    private final ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");
    private final SecureRandom random = new SecureRandom();

    public String generateTodayToken(Long memberId) {
        String date = LocalDate.now(zoneId).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE); // yyyyMMdd
        String payload = memberId + ":" + date;
        String signature = hmacSha256Hex(payload, secretKey);
        return "GQR:" + memberId + ":" + date + ":" + signature;
    }

    public Long verifyAndExtractMemberIdForToday(String token) {
        if (token == null || !token.startsWith("GQR:")) return null;
        String[] parts = token.split(":");
        if (parts.length != 4) return null;
        try {
            Long memberId = Long.parseLong(parts[1]);
            String date = parts[2];
            String sig = parts[3];
            String today = LocalDate.now(zoneId).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
            if (!today.equals(date)) return null; // chỉ hợp lệ trong ngày
            String expectedSig = hmacSha256Hex(memberId + ":" + date, secretKey);
            if (!constantTimeEquals(sig, expectedSig)) return null;
            return memberId;
        } catch (Exception e) {
            return null;
        }
    }

    private String hmacSha256Hex(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) result |= a.charAt(i) ^ b.charAt(i);
        return result == 0;
    }

    // QR chung của phòng gym, hợp lệ trong ngày
    public String generateGymTodayToken() {
        String date = LocalDate.now(zoneId).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String nonce = String.format("%06d", random.nextInt(1_000_000));
        String payload = "GYM:" + date + ":" + nonce;
        String signature = hmacSha256Hex(payload, secretKey);
        return "GQRD:" + date + ":" + nonce + ":" + signature;
    }

    public boolean verifyGymTodayToken(String token) {
        if (token == null || !token.startsWith("GQRD:")) return false;
        String[] parts = token.split(":");
        if (parts.length != 4) return false;
        String date = parts[1];
        String nonce = parts[2];
        String sig = parts[3];
        String today = LocalDate.now(zoneId).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        if (!today.equals(date)) return false;
        String expectedSig = hmacSha256Hex("GYM:" + date + ":" + nonce, secretKey);
        return constantTimeEquals(sig, expectedSig);
    }
}