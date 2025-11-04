package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.services.CheckInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/check-in")
@Tag(name = "Check-in API", description = "API để thực hiện check-in cho hội viên")
@SecurityRequirement(name = "bearerAuth")
public class CheckInController {

    private final CheckInService checkInService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Thực hiện check-in bằng mã vạch")
    public ResponseEntity<CheckInResponseDTO> checkIn(@Valid @RequestBody CheckInRequestDTO request) {
        CheckInResponseDTO response = checkInService.performCheckIn(request);
        return ResponseEntity.ok(response);
    }
}
