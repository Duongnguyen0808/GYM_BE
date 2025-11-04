package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO;
import com.gym.service.gymmanagementservice.dtos.WorkScheduleResponseDTO;
import com.gym.service.gymmanagementservice.services.WorkScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/schedules")
@Tag(name = "Work Schedule Management API", description = "Các API để quản lý lịch làm việc")
@SecurityRequirement(name = "bearerAuth")
public class WorkScheduleController {

    private final WorkScheduleService workScheduleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo lịch làm việc mới cho nhân viên (Chỉ Admin)")
    public ResponseEntity<WorkScheduleResponseDTO> createSchedule(@Valid @RequestBody WorkScheduleRequestDTO request) {
        WorkScheduleResponseDTO newSchedule = workScheduleService.createSchedule(request);
        return new ResponseEntity<>(newSchedule, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'PT')")
    @Operation(summary = "Xem lịch làm việc trong một khoảng thời gian (Tất cả nhân viên)")
    public ResponseEntity<List<WorkScheduleResponseDTO>> getSchedules(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {
        List<WorkScheduleResponseDTO> schedules = workScheduleService.getSchedules(start, end);
        return ResponseEntity.ok(schedules);
    }

    @DeleteMapping("/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa một lịch làm việc (Chỉ Admin)")
    public ResponseEntity<Void> deleteSchedule(@PathVariable Long scheduleId) {
        workScheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.noContent().build();
    }
}
