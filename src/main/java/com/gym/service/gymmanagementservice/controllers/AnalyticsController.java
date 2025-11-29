package com.gym.service.gymmanagementservice.controllers;

import com.gym.service.gymmanagementservice.services.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/metrics")
@Tag(name = "Analytics API", description = "API thống kê số liệu")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/check-ins")
    @Operation(summary = "Thống kê số lượt check-in trong khoảng thời gian")
    public ResponseEntity<Map<String, Long>> checkIns(
            @RequestParam("start") OffsetDateTime start,
            @RequestParam("end") OffsetDateTime end
    ) {
        return ResponseEntity.ok(analyticsService.getCheckInsStats(start, end));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Thống kê số gói mới/gia hạn/nâng cấp")
    public ResponseEntity<Map<String, Long>> subscriptions(
            @RequestParam("start") OffsetDateTime start,
            @RequestParam("end") OffsetDateTime end
    ) {
        return ResponseEntity.ok(analyticsService.getSubscriptionStats(start, end));
    }

    @GetMapping("/top-products")
    @Operation(summary = "Top sản phẩm bán chạy theo số lượng")
    public ResponseEntity<List<Map<String, Object>>> topProducts(
            @RequestParam("start") OffsetDateTime start,
            @RequestParam("end") OffsetDateTime end,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        return ResponseEntity.ok(analyticsService.getTopProducts(start, end, limit));
    }

    @GetMapping("/revenue-grouped")
    @Operation(summary = "Doanh thu nhóm theo tuần/tháng")
    public ResponseEntity<List<Map<String, Object>>> revenueGrouped(
            @RequestParam("start") OffsetDateTime start,
            @RequestParam("end") OffsetDateTime end,
            @RequestParam(value = "granularity", defaultValue = "week") String granularity
    ) {
        return ResponseEntity.ok(analyticsService.getRevenueGrouped(start, end, granularity));
    }
}
