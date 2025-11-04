package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.PtLogRequestDTO;
import com.gym.service.gymmanagementservice.models.PtSessionLog;
import com.gym.service.gymmanagementservice.services.PtSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pt-sessions")
@Tag(name = "PT Session Management API", description = "API để PT ghi lại (log) các buổi tập")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'PT')")
public class PtSessionController {

    private final PtSessionService ptSessionService;

    @PostMapping("/log/{memberPackageId}")
    @Operation(summary = "Ghi lại một buổi tập PT đã hoàn thành (Trừ 1 buổi)")
    public ResponseEntity<PtSessionLog> logSession(
            @PathVariable Long memberPackageId,
            @RequestBody(required = false) PtLogRequestDTO request) {

        String notes = (request != null) ? request.getNotes() : null;
        PtSessionLog loggedSession = ptSessionService.logPtSession(memberPackageId, notes);
        return ResponseEntity.ok(loggedSession);
    }
}