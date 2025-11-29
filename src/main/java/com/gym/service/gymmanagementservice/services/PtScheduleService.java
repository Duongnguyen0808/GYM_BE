package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PtScheduleService {

    private final PtScheduleRepository ptScheduleRepository;
    private final PtBookingRepository ptBookingRepository;
    private final PtSessionLogRepository ptSessionLogRepository;
    private final CheckInLogRepository checkInLogRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final UserRepository userRepository;
    private final AuthenticationService authenticationService;

    /**
     * PT tự quản lý lịch - đánh dấu available/unavailable
     */
    @Transactional
    public PtSchedule setScheduleAvailability(Long ptId, LocalDate scheduleDate, TimeSlot timeSlot, Boolean isAvailable, String notes) {
        // Kiểm tra quyền - chỉ PT đó mới được sửa lịch của mình
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if (!currentUser.getId().equals(ptId) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền thay đổi lịch của PT này.");
        }

        User pt = userRepository.findById(ptId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy PT với ID: " + ptId));

        if (pt.getRole() != Role.PT) {
            throw new IllegalArgumentException("Người dùng này không phải là PT.");
        }

        // Tìm hoặc tạo schedule
        PtSchedule schedule = ptScheduleRepository
                .findByPtIdAndScheduleDateAndTimeSlot(ptId, scheduleDate, timeSlot)
                .orElse(PtSchedule.builder()
                        .pt(pt)
                        .scheduleDate(scheduleDate)
                        .timeSlot(timeSlot)
                        .build());

        schedule.setIsAvailable(isAvailable);
        schedule.setNotes(notes);
        schedule.setUpdatedAt(OffsetDateTime.now());

        return ptScheduleRepository.save(schedule);
    }

    /**
     * Lấy lịch của PT trong khoảng thời gian
     */
    @Transactional(readOnly = true)
    public List<PtSchedule> getPtSchedule(Long ptId, LocalDate startDate, LocalDate endDate) {
        return ptScheduleRepository.findByPtIdAndScheduleDateBetween(ptId, startDate, endDate);
    }

    /**
     * Lấy lịch available của PT từ ngày hiện tại
     */
    @Transactional(readOnly = true)
    public List<PtSchedule> getAvailableSchedules(Long ptId, LocalDate fromDate) {
        return ptScheduleRepository.findAvailableSchedulesByPtIdAndDateFrom(ptId, fromDate);
    }

    /**
     * Lấy lịch theo tuần (calendar view) với thông tin booking và attendance
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWeeklySchedule(Long ptId, LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        List<PtSchedule> schedules = ptScheduleRepository.findByPtIdAndScheduleDateBetween(ptId, weekStart, weekEnd);
        
        // Lấy tất cả MemberPackage có PT này, status ACTIVE, và có timeSlot (gói PT với lịch cố định)
        // Sử dụng query tối ưu thay vì findAll() rồi filter
        List<MemberPackage> activeMemberPackages = memberPackageRepository
                .findByAssignedPtIdAndStatusAndGymPackage_PackageTypeAndTimeSlotIsNotNullAndStartDateIsNotNull(
                        ptId, SubscriptionStatus.ACTIVE, PackageType.PT_SESSION);
        
        log.debug("PT {} - Found {} active member packages for week {} to {}", 
                ptId, activeMemberPackages.size(), weekStart, weekEnd);
        
        // Log chi tiết từng package để debug
        for (MemberPackage mp : activeMemberPackages) {
            log.debug("Package: member={}, timeSlot={}, allowedWeekdays={}, startDate={}, endDate={}", 
                    mp.getMember().getFullName(), mp.getTimeSlot(), mp.getAllowedWeekdays(), 
                    mp.getStartDate(), mp.getEndDate());
        }
        
        // Lấy bookings trong tuần (để hiển thị booking riêng lẻ nếu có)
        OffsetDateTime weekStartTime = weekStart.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime weekEndTime = weekEnd.atTime(23, 59, 59).atOffset(OffsetDateTime.now().getOffset());
        List<PtBooking> bookings = ptBookingRepository.findByPtIdAndBookingDateBetween(ptId, weekStart, weekEnd);
        
        // Lấy session logs (đã hoàn thành) trong tuần
        List<PtSessionLog> sessionLogs = ptSessionLogRepository.findByPtUserId(ptId).stream()
                .filter(log -> {
                    LocalDate logDate = log.getSessionDate().toLocalDate();
                    return !logDate.isBefore(weekStart) && !logDate.isAfter(weekEnd);
                })
                .collect(Collectors.toList());
        
        // Lấy check-in logs để biết học viên có đi tập hay không (chỉ lấy của PT này)
        List<CheckInLog> allCheckInLogs = checkInLogRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(
                weekStartTime, weekEndTime, org.springframework.data.domain.PageRequest.of(0, 1000));
        
        // Lọc chỉ lấy check-in của học viên có gói PT với PT này
        List<CheckInLog> checkInLogs = allCheckInLogs.stream()
                .filter(log -> log.getMemberPackage() != null 
                        && log.getMemberPackage().getAssignedPt() != null
                        && log.getMemberPackage().getAssignedPt().getId().equals(ptId))
                .collect(Collectors.toList());

        // Nhóm theo ngày và time slot - sử dụng string key để dễ truy cập trong Thymeleaf
        Map<String, Map<String, PtSchedule>> scheduleMap = new HashMap<>();
        Map<String, Map<String, List<Map<String, Object>>>> bookingsMap = new HashMap<>();
        Map<String, Map<String, Boolean>> attendanceMap = new HashMap<>();
        
        // Sử dụng formatter để đảm bảo format dateKey nhất quán với template
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        Map<Long, Integer> weeklySessionCounters = new HashMap<>(); // packageId -> số buổi đã xếp trong tuần (để hiển thị còn bao nhiêu)

        for (TimeSlot slot : TimeSlot.values()) {
            for (int i = 0; i < 7; i++) {
                LocalDate date = weekStart.plusDays(i);
                String dateKey = date.format(dateFormatter);
                String slotKey = slot.name();
                
                scheduleMap.putIfAbsent(dateKey, new HashMap<>());
                bookingsMap.putIfAbsent(dateKey, new HashMap<>());
                attendanceMap.putIfAbsent(dateKey, new HashMap<>());
                
                PtSchedule schedule = schedules.stream()
                        .filter(s -> s.getScheduleDate().equals(date) && s.getTimeSlot() == slot)
                        .findFirst()
                        .orElse(null);
                
                scheduleMap.get(dateKey).put(slotKey, schedule);
                
                // Lấy tất cả học viên có gói PT với timeSlot này và ngày này nằm trong khoảng startDate-endDate
                List<Map<String, Object>> slotStudents = new java.util.ArrayList<>();
                
                // Lấy từ MemberPackage (gói PT với lịch cố định)
                for (MemberPackage mp : activeMemberPackages) {
                    if (mp.getTimeSlot() == slot) {
                        // Chuyển đổi OffsetDateTime sang LocalDate với timezone Asia/Ho_Chi_Minh
                        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Ho_Chi_Minh");
                        LocalDate startDate = mp.getStartDate().atZoneSameInstant(zoneId).toLocalDate();
                        LocalDate endDate = mp.getEndDate() != null 
                                ? mp.getEndDate().atZoneSameInstant(zoneId).toLocalDate() 
                                : null;
                        
                        // Kiểm tra ngày này có nằm trong khoảng thời hạn gói không
                        // Bao gồm cả ngày bắt đầu và kết thúc (date >= startDate && date <= endDate)
                        // Sử dụng !isBefore và !isAfter để bao gồm cả ngày bắt đầu và kết thúc
                        boolean isInRange = !date.isBefore(startDate) && 
                                           (endDate == null || !date.isAfter(endDate));
                        
                        // Kiểm tra ngày này có phải là thứ đã chọn không (nếu có allowedWeekdays)
                        boolean isAllowedWeekday = true;
                        if (mp.getAllowedWeekdays() != null && !mp.getAllowedWeekdays().isBlank()) {
                            String[] allowedDays = mp.getAllowedWeekdays().split(",");
                            String currentDayFull = date.getDayOfWeek().name(); // MONDAY, TUESDAY,...
                            String currentDayShort = currentDayFull.substring(0, 3); // MON, TUE,...

                            java.util.List<String> normalizedAllowedDays = java.util.Arrays.stream(allowedDays)
                                    .map(String::trim)
                                    .map(String::toUpperCase)
                                    .collect(Collectors.toList());

                            isAllowedWeekday = normalizedAllowedDays.contains(currentDayFull)
                                    || normalizedAllowedDays.contains(currentDayShort);
                        }
                        
                        // Log để debug
                        if (mp.getMember() != null) {
                            log.debug("Checking student {}: date={}, slot={}, startDate={}, endDate={}, allowedWeekdays={}, isInRange={}, isAllowedWeekday={}", 
                                    mp.getMember().getFullName(), date, slot, startDate, endDate, mp.getAllowedWeekdays(), isInRange, isAllowedWeekday);
                        }
                        
                        // Thêm học viên vào lịch nếu thỏa mãn điều kiện
                        if (isInRange && isAllowedWeekday) {
                            log.info("✓ Adding student {} to schedule: date={}, slot={}", 
                                    mp.getMember().getFullName(), date, slot);
                            Long memberId = mp.getMember().getId();
                            Long packageId = mp.getId();

                            int remainingSessions = mp.getRemainingSessions() != null ? mp.getRemainingSessions() : 0;
                            int usedThisWeek = weeklySessionCounters.getOrDefault(packageId, 0);
                            int displayRemaining = Math.max(remainingSessions - usedThisWeek, 0);
                            weeklySessionCounters.put(packageId, usedThisWeek + 1);
                            
                            // Kiểm tra attendance cho học viên này trong ngày này
                            boolean hasAttendance = sessionLogs.stream()
                                    .anyMatch(log -> {
                                        LocalDate logDate = log.getSessionDate().toLocalDate();
                                        return logDate.equals(date) && 
                                               log.getMemberPackage() != null &&
                                               log.getMemberPackage().getMember() != null &&
                                               log.getMemberPackage().getMember().getId().equals(memberId);
                                    }) || checkInLogs.stream()
                                    .anyMatch(log -> {
                                        LocalDate logDate = log.getCheckInTime().toLocalDate();
                                        return logDate.equals(date) && 
                                               log.getMemberPackage() != null &&
                                               log.getMemberPackage().getMember() != null &&
                                               log.getMemberPackage().getMember().getId().equals(memberId);
                                    });
                            
                            Map<String, Object> studentInfo = new HashMap<>();
                            studentInfo.put("memberName", mp.getMember().getFullName());
                            studentInfo.put("memberId", memberId);
                            studentInfo.put("packageId", packageId);
                            studentInfo.put("remainingSessions", mp.getRemainingSessions());
                            studentInfo.put("remainingSessionsDisplay", displayRemaining);
                            studentInfo.put("hasAttendance", hasAttendance);
                            studentInfo.put("startDate", mp.getStartDate());
                            studentInfo.put("endDate", mp.getEndDate());
                            slotStudents.add(studentInfo);
                        }
                    }
                }
                
                // Thêm bookings riêng lẻ (nếu có) - những booking không phải từ gói cố định
                List<Map<String, Object>> slotBookings = bookings.stream()
                        .filter(b -> b.getBookingDate().equals(date) && b.getTimeSlot() == slot)
                        .filter(b -> b.getStatus() == BookingStatus.CONFIRMED || b.getStatus() == BookingStatus.PENDING)
                        .filter(b -> {
                            // Chỉ thêm booking nếu chưa có trong slotStudents (tránh trùng)
                            Long packageId = b.getMemberPackage().getId();
                            return slotStudents.stream().noneMatch(s -> s.get("packageId").equals(packageId));
                        })
                        .map(b -> {
                            Long memberId = b.getMemberPackage().getMember().getId();
                            Long packageId = b.getMemberPackage().getId();
                            
                            int remainingSessions = b.getMemberPackage().getRemainingSessions() != null
                                    ? b.getMemberPackage().getRemainingSessions()
                                    : 0;
                            int usedThisWeek = weeklySessionCounters.getOrDefault(packageId, 0);
                            int displayRemaining = Math.max(remainingSessions - usedThisWeek, 0);
                            weeklySessionCounters.put(packageId, usedThisWeek + 1);

                            // Kiểm tra attendance cho học viên này
                            boolean hasAttendance = sessionLogs.stream()
                                    .anyMatch(log -> {
                                        LocalDate logDate = log.getSessionDate().toLocalDate();
                                        return logDate.equals(date) && 
                                               log.getMemberPackage().getMember().getId().equals(memberId);
                                    }) || checkInLogs.stream()
                                    .anyMatch(log -> {
                                        LocalDate logDate = log.getCheckInTime().toLocalDate();
                                        return logDate.equals(date) && 
                                               log.getMemberPackage() != null &&
                                               log.getMemberPackage().getMember().getId().equals(memberId);
                                    });
                            
                            Map<String, Object> bookingInfo = new HashMap<>();
                            bookingInfo.put("id", b.getId());
                            bookingInfo.put("memberName", b.getMemberPackage().getMember().getFullName());
                            bookingInfo.put("memberId", memberId);
                            bookingInfo.put("packageId", packageId);
                            bookingInfo.put("remainingSessionsDisplay", displayRemaining);
                            bookingInfo.put("status", b.getStatus().name());
                            bookingInfo.put("notes", b.getNotes());
                            bookingInfo.put("hasAttendance", hasAttendance);
                            return bookingInfo;
                        })
                        .collect(Collectors.toList());
                
                // Gộp cả students và bookings
                slotStudents.addAll(slotBookings);
                
                bookingsMap.get(dateKey).put(slotKey, slotStudents);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("weekStart", weekStart);
        result.put("weekEnd", weekEnd);
        result.put("schedules", scheduleMap);
        result.put("bookings", bookingsMap);
        result.put("timeSlots", TimeSlot.values());

        return result;
    }

    /**
     * Xóa lịch cũ (trước ngày hiện tại)
     */
    @Transactional
    public void cleanupOldSchedules(Long ptId, LocalDate beforeDate) {
        ptScheduleRepository.deleteByPtIdAndScheduleDateBefore(ptId, beforeDate);
    }

    /**
     * Batch update lịch cho nhiều ngày
     */
    @Transactional
    public List<PtSchedule> batchUpdateSchedule(Long ptId, LocalDate startDate, LocalDate endDate, 
                                                 TimeSlot timeSlot, Boolean isAvailable, String notes) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if (!currentUser.getId().equals(ptId) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền thay đổi lịch của PT này.");
        }

        List<PtSchedule> schedules = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> {
                    PtSchedule schedule = ptScheduleRepository
                            .findByPtIdAndScheduleDateAndTimeSlot(ptId, date, timeSlot)
                            .orElse(PtSchedule.builder()
                                    .pt(userRepository.findById(ptId).orElseThrow())
                                    .scheduleDate(date)
                                    .timeSlot(timeSlot)
                                    .build());

                    schedule.setIsAvailable(isAvailable);
                    schedule.setNotes(notes);
                    schedule.setUpdatedAt(OffsetDateTime.now());

                    return schedule;
                })
                .collect(Collectors.toList());

        return ptScheduleRepository.saveAll(schedules);
    }
}

