package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.models.PtBooking;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import com.gym.service.gymmanagementservice.services.PtBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pt-bookings")
@Tag(name = "PT Booking Management API", description = "API để đặt/hủy/đổi lịch tập với PT")
@SecurityRequirement(name = "bearerAuth")
public class PtBookingController {

    private final PtBookingService ptBookingService;

    @PostMapping("/create")
    @Operation(summary = "Học viên đặt lịch tập với PT")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MEMBER')")
    public ResponseEntity<PtBooking> createBooking(
            @RequestBody CreateBookingRequest request) {
        
        PtBooking booking = ptBookingService.createBooking(
                request.getMemberPackageId(),
                request.getBookingDate(),
                request.getTimeSlot(),
                request.getNotes()
        );
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/confirm")
    @Operation(summary = "PT xác nhận booking")
    @PreAuthorize("hasAnyRole('ADMIN', 'PT')")
    public ResponseEntity<PtBooking> confirmBooking(@PathVariable Long bookingId) {
        PtBooking booking = ptBookingService.confirmBooking(bookingId);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/cancel")
    @Operation(summary = "Hủy booking")
    public ResponseEntity<PtBooking> cancelBooking(
            @PathVariable Long bookingId,
            @RequestBody CancelBookingRequest request) {
        
        PtBooking booking = ptBookingService.cancelBooking(bookingId, request.getReason(), request.getCancelledBy());
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/complete")
    @Operation(summary = "Đánh dấu booking đã hoàn thành")
    @PreAuthorize("hasAnyRole('ADMIN', 'PT')")
    public ResponseEntity<PtBooking> completeBooking(
            @PathVariable Long bookingId,
            @RequestBody(required = false) CompleteBookingRequest request) {
        
        String notes = request != null ? request.getNotes() : null;
        PtBooking booking = ptBookingService.completeBooking(bookingId, notes);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/pt/{ptId}")
    @Operation(summary = "Lấy danh sách booking của PT")
    @PreAuthorize("hasAnyRole('ADMIN', 'PT')")
    public ResponseEntity<List<PtBooking>> getPtBookings(
            @PathVariable Long ptId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusDays(30);
        }
        
        List<PtBooking> bookings = ptBookingService.getPtBookings(ptId, startDate, endDate);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/pt/{ptId}/upcoming")
    @Operation(summary = "Lấy booking sắp tới của PT")
    @PreAuthorize("hasAnyRole('ADMIN', 'PT')")
    public ResponseEntity<List<PtBooking>> getUpcomingPtBookings(@PathVariable Long ptId) {
        List<PtBooking> bookings = ptBookingService.getUpcomingPtBookings(ptId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/member/{memberId}")
    @Operation(summary = "Lấy danh sách booking của học viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MEMBER')")
    public ResponseEntity<List<PtBooking>> getMemberBookings(@PathVariable Long memberId) {
        List<PtBooking> bookings = ptBookingService.getMemberBookings(memberId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/member/{memberId}/upcoming")
    @Operation(summary = "Lấy booking sắp tới của học viên")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MEMBER')")
    public ResponseEntity<List<PtBooking>> getUpcomingMemberBookings(@PathVariable Long memberId) {
        List<PtBooking> bookings = ptBookingService.getUpcomingMemberBookings(memberId);
        return ResponseEntity.ok(bookings);
    }

    @Data
    static class CreateBookingRequest {
        private Long memberPackageId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate bookingDate;
        private TimeSlot timeSlot;
        private String notes;
    }

    @Data
    static class CancelBookingRequest {
        private String reason;
        private String cancelledBy;
    }

    @Data
    static class CompleteBookingRequest {
        private String notes;
    }
}






