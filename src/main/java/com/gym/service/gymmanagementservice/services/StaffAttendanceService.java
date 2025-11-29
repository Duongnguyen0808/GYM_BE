package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.StaffAttendance;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.StaffAttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StaffAttendanceService {
    private final StaffAttendanceRepository attendanceRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public StaffAttendance checkIn() {
        User current = authenticationService.getCurrentAuthenticatedUser();
        if (current.getRole() != Role.ADMIN && current.getRole() != Role.STAFF && current.getRole() != Role.PT) {
            throw new IllegalStateException("Chỉ nhân viên/PT mới được chấm công.");
        }
        attendanceRepository.findTopByUser_IdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(current.getId())
                .ifPresent(a -> { throw new IllegalStateException("Bạn đang có một ca làm chưa check-out."); });

        StaffAttendance att = StaffAttendance.builder()
                .user(current)
                .checkInTime(OffsetDateTime.now())
                .build();
        return attendanceRepository.save(att);
    }

    @Transactional
    public StaffAttendance checkOut() {
        User current = authenticationService.getCurrentAuthenticatedUser();
        StaffAttendance att = attendanceRepository.findTopByUser_IdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(current.getId())
                .orElseThrow(() -> new IllegalStateException("Không có ca làm nào để check-out."));
        OffsetDateTime now = OffsetDateTime.now();
        att.setCheckOutTime(now);
        long seconds = java.time.Duration.between(att.getCheckInTime(), now).getSeconds();
        att.setDurationSeconds(seconds);
        return attendanceRepository.save(att);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> reportHours(Long userId, OffsetDateTime start, OffsetDateTime end) {
        List<StaffAttendance> records = attendanceRepository.findByUser_IdAndCheckInTimeBetween(userId, start, end);
        long totalSeconds = records.stream().mapToLong(r -> r.getDurationSeconds() != null ? r.getDurationSeconds() : 0L).sum();
        Map<String, Object> res = new HashMap<>();
        res.put("userId", userId);
        res.put("totalSeconds", totalSeconds);
        res.put("totalHours", totalSeconds / 3600.0);
        res.put("records", records);
        return res;
    }
}
