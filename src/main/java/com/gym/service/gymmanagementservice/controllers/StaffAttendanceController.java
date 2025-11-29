package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.models.StaffAttendance;
import com.gym.service.gymmanagementservice.services.StaffAttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/staff-attendance")
@Tag(name = "Staff Attendance API", description = "API chấm công nhân viên")
@SecurityRequirement(name = "bearerAuth")
public class StaffAttendanceController {

    private final StaffAttendanceService staffAttendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','PT')")
    @Operation(summary = "Nhân viên/PT check-in")
    public ResponseEntity<StaffAttendance> checkIn() {
        return ResponseEntity.ok(staffAttendanceService.checkIn());
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','PT')")
    @Operation(summary = "Nhân viên/PT check-out")
    public ResponseEntity<StaffAttendance> checkOut() {
        return ResponseEntity.ok(staffAttendanceService.checkOut());
    }

    @GetMapping("/report")
    @PreAuthorize("hasAnyRole('ADMIN')")
    @Operation(summary = "Báo cáo giờ làm việc của nhân viên")
    public ResponseEntity<Map<String, Object>> report(@RequestParam("userId") Long userId,
                                                      @RequestParam("start") OffsetDateTime start,
                                                      @RequestParam("end") OffsetDateTime end) {
        return ResponseEntity.ok(staffAttendanceService.reportHours(userId, start, end));
    }
}
