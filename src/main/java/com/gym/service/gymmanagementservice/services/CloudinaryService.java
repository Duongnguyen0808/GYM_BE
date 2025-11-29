package com.gym.service.gymmanagementservice.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Map;

@Service
public class CloudinaryService {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;
    @Value("${cloudinary.api-key:}")
    private String apiKey;
    @Value("${cloudinary.api-secret:}")
    private String apiSecret;
    @Value("${cloudinary.upload-preset:}")
    private String uploadPreset;

            public String upload(MultipartFile file) {
                if (file == null || file.isEmpty()) return null;
                if (cloudName == null || cloudName.isBlank()) {
                    throw new IllegalStateException("Cloudinary cloud-name chưa được cấu hình.");
                }
                final String url = "https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload";
                final RestTemplate restTemplate = new RestTemplate();

                MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                org.springframework.core.io.ByteArrayResource fileResource;
                try {
                    fileResource = new org.springframework.core.io.ByteArrayResource(file.getBytes()) {
                        @Override
                        public String getFilename() { return file.getOriginalFilename(); }
                    };
                } catch (Exception ex) {
                    throw new IllegalStateException("Không đọc được nội dung file: " + ex.getMessage());
                }
                body.add("file", fileResource);

        if (uploadPreset != null && !uploadPreset.isBlank()) {
            body.add("upload_preset", uploadPreset);
        } else if (apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank()) {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String toSign = "timestamp=" + timestamp + apiSecret;
            String signature = sha1Hex(toSign);
            body.add("api_key", apiKey);
            body.add("timestamp", timestamp);
            body.add("signature", signature);
        } else {
            throw new IllegalStateException("Cloudinary chưa được cấu hình đúng (preset hoặc apiKey+apiSecret).");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> req = new HttpEntity<>(body, headers);

                Map<?, ?> res = restTemplate.postForObject(url, req, Map.class);
        if (res == null) throw new IllegalStateException("Cloudinary phản hồi rỗng");
        Object secureUrl = res.get("secure_url");
        Object plainUrl = res.get("url");
        String finalUrl = secureUrl != null ? secureUrl.toString() : (plainUrl != null ? plainUrl.toString() : null);
        if (finalUrl == null || finalUrl.isBlank()) throw new IllegalStateException("Không nhận được URL ảnh từ Cloudinary");
        return finalUrl;
    }

    private String sha1Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}