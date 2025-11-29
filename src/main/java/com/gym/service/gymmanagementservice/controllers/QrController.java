package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.services.DailyQrService;
import com.gym.service.gymmanagementservice.services.MemberService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
@Tag(name = "QR Code API", description = "API tạo QR hằng ngày cho hội viên")
@SecurityRequirement(name = "bearerAuth")
public class QrController {

    private final DailyQrService dailyQrService;
    private final MemberService memberService;

    @GetMapping("/{memberId}/qr/token/today")
    @Operation(summary = "Lấy token QR hợp lệ trong ngày cho hội viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, String>> getTodayQrToken(@PathVariable Long memberId) {
        // Đảm bảo member tồn tại
        memberService.getMemberById(memberId);
        String token = dailyQrService.generateTodayToken(memberId);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping(value = "/{memberId}/qr/image/today", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Trả về ảnh PNG QR cho token hằng ngày")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public @ResponseBody byte[] getTodayQrImage(@PathVariable Long memberId) throws WriterException, IOException {
        // Đảm bảo member tồn tại
        memberService.getMemberById(memberId);
        String token = dailyQrService.generateTodayToken(memberId);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(token, BarcodeFormat.QR_CODE, 280, 280);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }

    @GetMapping("/qr/gym/token/today")
    @Operation(summary = "Lấy token QR chung của phòng gym, hợp lệ trong ngày")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Map<String, String>> getGymTodayQrToken() {
        String token = dailyQrService.generateGymTodayToken();
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping(value = "/qr/gym/image/today", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Trả về ảnh PNG QR cho token chung hằng ngày của phòng gym")
    @PreAuthorize("permitAll()")
    public @ResponseBody byte[] getGymTodayQrImage() throws WriterException, IOException {
        String token = dailyQrService.generateGymTodayToken();
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        String scanUrl = baseUrl + "/scan?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(scanUrl, BarcodeFormat.QR_CODE, 512, 512);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
        return baos.toByteArray();
    }
}