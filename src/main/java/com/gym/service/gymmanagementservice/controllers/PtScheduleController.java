package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.models.PtSchedule;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import com.gym.service.gymmanagementservice.services.PtScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pt-schedules")
@Tag(name = "PT Schedule Management API", description = "API để PT quản lý lịch available/unavailable")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'PT')")
public class PtScheduleController {

    private final PtScheduleService ptScheduleService;

    @PostMapping("/{ptId}/set-availability")
    @Operation(summary = "PT đánh dấu available/unavailable cho một ngày và time slot")
    public ResponseEntity<PtSchedule> setAvailability(
            @PathVariable Long ptId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam TimeSlot timeSlot,
            @RequestParam Boolean isAvailable,
            @RequestParam(required = false) String notes) {
        
        PtSchedule schedule = ptScheduleService.setScheduleAvailability(ptId, scheduleDate, timeSlot, isAvailable, notes);
        return ResponseEntity.ok(schedule);
    }

    @GetMapping("/{ptId}")
    @Operation(summary = "Lấy lịch của PT trong khoảng thời gian")
    public ResponseEntity<List<PtSchedule>> getSchedule(
            @PathVariable Long ptId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<PtSchedule> schedules = ptScheduleService.getPtSchedule(ptId, startDate, endDate);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{ptId}/available")
    @Operation(summary = "Lấy lịch available của PT từ ngày hiện tại")
    public ResponseEntity<List<PtSchedule>> getAvailableSchedules(
            @PathVariable Long ptId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate) {
        
        if (fromDate == null) {
            fromDate = LocalDate.now();
        }
        
        List<PtSchedule> schedules = ptScheduleService.getAvailableSchedules(ptId, fromDate);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/{ptId}/weekly")
    @Operation(summary = "Lấy lịch theo tuần (calendar view)")
    public ResponseEntity<Map<String, Object>> getWeeklySchedule(
            @PathVariable Long ptId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        
        Map<String, Object> schedule = ptScheduleService.getWeeklySchedule(ptId, weekStart);
        return ResponseEntity.ok(schedule);
    }

    @PostMapping("/{ptId}/batch-update")
    @Operation(summary = "Batch update lịch cho nhiều ngày")
    public ResponseEntity<List<PtSchedule>> batchUpdateSchedule(
            @PathVariable Long ptId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam TimeSlot timeSlot,
            @RequestParam Boolean isAvailable,
            @RequestParam(required = false) String notes) {
        
        List<PtSchedule> schedules = ptScheduleService.batchUpdateSchedule(ptId, startDate, endDate, timeSlot, isAvailable, notes);
        return ResponseEntity.ok(schedules);
    }
}






