package com.gym.service.gymmanagementservice.controllers.web;

import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.services.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/pt")
@PreAuthorize("hasRole('PT')")
public class PtWebController {

    private final PtManagementService ptManagementService;
    private final PtScheduleService ptScheduleService;
    private final PtBookingService ptBookingService;
    private final PtSessionService ptSessionService;
    private final AuthenticationService authenticationService;

    /**
     * Dashboard chính của PT - Calendar view và quản lý lịch
     */
    @GetMapping("/dashboard")
    public String dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Model model) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        
        if (weekStart == null) {
            weekStart = LocalDate.now();
            // Lấy thứ 2 của tuần
            int dayOfWeek = weekStart.getDayOfWeek().getValue();
            weekStart = weekStart.minusDays(dayOfWeek - 1);
        }

        // Lấy thống kê tổng quan
        Map<String, Object> stats = ptManagementService.getPtStatistics(currentPt.getId());
        
        // Lấy lịch theo tuần
        Map<String, Object> weeklySchedule = ptScheduleService.getWeeklySchedule(currentPt.getId(), weekStart);

        model.addAttribute("stats", stats);
        model.addAttribute("weeklySchedule", weeklySchedule);
        model.addAttribute("currentPt", currentPt);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("pageTitle", "Dashboard PT");
        model.addAttribute("contentView", "pt/dashboard");
        model.addAttribute("activePage", "ptDashboard");

        return "fragments/layout";
    }

    /**
     * Quản lý lịch - Set available/unavailable
     */
    @GetMapping("/schedule")
    public String scheduleManagement(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            Model model) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        
        if (weekStart == null) {
            weekStart = LocalDate.now();
            int dayOfWeek = weekStart.getDayOfWeek().getValue();
            weekStart = weekStart.minusDays(dayOfWeek - 1);
        }

        Map<String, Object> weeklySchedule = ptScheduleService.getWeeklySchedule(currentPt.getId(), weekStart);

        model.addAttribute("weeklySchedule", weeklySchedule);
        model.addAttribute("currentPt", currentPt);
        model.addAttribute("weekStart", weekStart);
        model.addAttribute("pageTitle", "Quản lý Lịch");
        model.addAttribute("contentView", "pt/schedule");
        model.addAttribute("activePage", "ptSchedule");

        return "fragments/layout";
    }

    @PostMapping("/schedule/set-availability")
    public String setAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduleDate,
            @RequestParam TimeSlot timeSlot,
            @RequestParam Boolean isAvailable,
            @RequestParam(required = false) String notes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart,
            RedirectAttributes redirectAttributes) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        
        try {
            ptScheduleService.setScheduleAvailability(
                    currentPt.getId(),
                    scheduleDate,
                    timeSlot,
                    isAvailable,
                    notes
            );
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật lịch thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        if (weekStart != null) {
            return "redirect:/pt/schedule?weekStart=" + weekStart;
        }
        return "redirect:/pt/schedule";
    }

    /**
     * Danh sách học viên đang theo
     */
    @GetMapping("/students")
    public String students(
            @RequestParam(required = false) String status,
            Model model) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        Map<String, Object> stats = ptManagementService.getPtStatistics(currentPt.getId());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allStudents = (List<Map<String, Object>>) stats.get("activeStudentsList");
        
        // Lọc theo trạng thái nếu có
        if (status != null && !status.isEmpty()) {
            // Có thể filter thêm logic ở đây nếu cần
        }

        model.addAttribute("students", allStudents);
        model.addAttribute("stats", stats);
        model.addAttribute("currentPt", currentPt);
        model.addAttribute("statusFilter", status);
        model.addAttribute("pageTitle", "Danh sách Học viên");
        model.addAttribute("contentView", "pt/students");
        model.addAttribute("activePage", "ptStudents");

        return "fragments/layout";
    }

    /**
     * Chi tiết học viên
     */
    @GetMapping("/students/{memberPackageId}")
    public String studentDetail(
            @PathVariable Long memberPackageId,
            Model model) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        Map<String, Object> stats = ptManagementService.getPtStatistics(currentPt.getId());
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allStudents = (List<Map<String, Object>>) stats.get("activeStudentsList");
        
        Map<String, Object> student = allStudents.stream()
                .filter(s -> s.get("packageId").equals(memberPackageId))
                .findFirst()
                .orElse(null);

        if (student == null) {
            return "redirect:/pt/students";
        }

        model.addAttribute("student", student);
        model.addAttribute("currentPt", currentPt);
        model.addAttribute("pageTitle", "Chi tiết Học viên");
        model.addAttribute("contentView", "pt/student-detail");
        model.addAttribute("activePage", "ptStudents");

        return "fragments/layout";
    }

    /**
     * Quản lý booking
     */
    @GetMapping("/bookings")
    public String bookings(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        
        User currentPt = authenticationService.getCurrentAuthenticatedUser();
        
        if (startDate == null) {
            startDate = LocalDate.now();
        }
        if (endDate == null) {
            endDate = startDate.plusDays(30);
        }

        List<PtBooking> bookings = ptBookingService.getPtBookings(currentPt.getId(), startDate, endDate);

        model.addAttribute("bookings", bookings);
        model.addAttribute("currentPt", currentPt);
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("pageTitle", "Quản lý Booking");
        model.addAttribute("contentView", "pt/bookings");
        model.addAttribute("activePage", "ptBookings");

        return "fragments/layout";
    }

    @PostMapping("/bookings/{bookingId}/confirm")
    public String confirmBooking(
            @PathVariable Long bookingId,
            RedirectAttributes redirectAttributes) {
        
        try {
            ptBookingService.confirmBooking(bookingId);
            redirectAttributes.addFlashAttribute("successMessage", "Xác nhận booking thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/pt/bookings";
    }

    @PostMapping("/bookings/{bookingId}/cancel")
    public String cancelBooking(
            @PathVariable Long bookingId,
            @RequestParam String reason,
            RedirectAttributes redirectAttributes) {
        
        try {
            ptBookingService.cancelBooking(bookingId, reason, "PT");
            redirectAttributes.addFlashAttribute("successMessage", "Hủy booking thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/pt/bookings";
    }

    @PostMapping("/bookings/{bookingId}/complete")
    public String completeBooking(
            @PathVariable Long bookingId,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        try {
            ptBookingService.completeBooking(bookingId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Đánh dấu hoàn thành thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/pt/bookings";
    }

    /**
     * Ghi nhận buổi tập đã hoàn thành
     */
    @PostMapping("/sessions/log/{memberPackageId}")
    public String logSession(
            @PathVariable Long memberPackageId,
            @RequestParam(required = false) String notes,
            RedirectAttributes redirectAttributes) {
        
        try {
            ptSessionService.logPtSession(memberPackageId, notes);
            redirectAttributes.addFlashAttribute("successMessage", "Ghi nhận buổi tập thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        
        return "redirect:/pt/students/" + memberPackageId;
    }
}

