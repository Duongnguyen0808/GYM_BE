package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.services.CheckInService;
import com.gym.service.gymmanagementservice.services.DailyQrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/check-in")
@Tag(name = "Check-in API", description = "API để thực hiện check-in cho hội viên")
@SecurityRequirement(name = "bearerAuth")
public class CheckInController {

    private final CheckInService checkInService;
    private final DailyQrService dailyQrService;

    private final List<SseEmitter> emitters = Collections.synchronizedList(new ArrayList<>());

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Thực hiện check-in bằng mã vạch")
    public ResponseEntity<CheckInResponseDTO> checkIn(@Valid @RequestBody CheckInRequestDTO request) {
        CheckInResponseDTO response = checkInService.performCheckIn(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/events")
    @Operation(summary = "Luồng SSE nhận kết quả check-in từ điện thoại")
    public SseEmitter subscribeEvents() {
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {}
        return emitter;
    }

    @PostMapping("/mobile")
    @Operation(summary = "Điện thoại gửi kết quả quét QR chung kèm SĐT để check-in")
    public ResponseEntity<CheckInResponseDTO> mobileCheckIn(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String phone = body.get("phone");
        if (token == null || phone == null || phone.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        boolean valid = dailyQrService.verifyGymTodayToken(token);
        if (!valid) {
            CheckInResponseDTO invalid = CheckInResponseDTO.builder()
                    .status(com.gym.service.gymmanagementservice.models.CheckInStatus.FAILED_MEMBER_NOT_FOUND)
                    .message("QR không hợp lệ hoặc đã hết hạn")
                    .build();
            broadcast(invalid);
            return ResponseEntity.ok(invalid);
        }
        CheckInRequestDTO req = new CheckInRequestDTO();
        req.setBarcode(phone);
        CheckInResponseDTO response = checkInService.performCheckIn(req);
        broadcast(response);
        return ResponseEntity.ok(response);
    }

    private void broadcast(CheckInResponseDTO response) {
        List<SseEmitter> failed = new ArrayList<>();
        synchronized (emitters) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("check-in")
                            .data(response));
                } catch (IOException e) {
                    failed.add(emitter);
                }
            }
            emitters.removeAll(failed);
        }
    }

    @PostMapping("/mobile/subscription")
    @Operation(summary = "Điện thoại gửi token phòng gym kèm subscriptionId và action IN/OUT")
    public ResponseEntity<CheckInResponseDTO> mobileCheckInBySubscription(@RequestBody Map<String, String> body) {
        String token = body.get("token");
        String subscriptionIdStr = body.get("subscriptionId");
        String action = body.getOrDefault("action", "IN");
        if (token == null || subscriptionIdStr == null) {
            return ResponseEntity.badRequest().build();
        }
        boolean valid = dailyQrService.verifyGymTodayToken(token);
        if (!valid) {
            CheckInResponseDTO invalid = CheckInResponseDTO.builder()
                    .status(com.gym.service.gymmanagementservice.models.CheckInStatus.FAILED_MEMBER_NOT_FOUND)
                    .message("QR không hợp lệ hoặc đã hết hạn")
                    .build();
            broadcast(invalid);
            return ResponseEntity.ok(invalid);
        }
        Long subscriptionId;
        try { subscriptionId = Long.parseLong(subscriptionIdStr); } catch (Exception e) { return ResponseEntity.badRequest().build(); }
        CheckInResponseDTO response;
        if ("OUT".equalsIgnoreCase(action)) {
            response = checkInService.performCheckoutBySubscription(subscriptionId);
        } else {
            response = checkInService.performCheckInBySubscription(subscriptionId);
        }
        broadcast(response);
        return ResponseEntity.ok(response);
    }
}
