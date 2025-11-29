package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.WorkScheduleRequestDTO;
import com.gym.service.gymmanagementservice.dtos.WorkScheduleResponseDTO;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.models.WorkSchedule;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import com.gym.service.gymmanagementservice.repositories.WorkScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkScheduleService {

    private final WorkScheduleRepository workScheduleRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorkScheduleResponseDTO createSchedule(WorkScheduleRequestDTO request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy nhân viên với ID: " + request.getUserId()));

        if (request.getStartTime().isAfter(request.getEndTime())) {
            throw new IllegalArgumentException("Thời gian bắt đầu không thể sau thời gian kết thúc.");
        }

        ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");
        OffsetDateTime start = request.getStartTime().atZone(zone).toOffsetDateTime();
        OffsetDateTime end = request.getEndTime().atZone(zone).toOffsetDateTime();

        WorkSchedule schedule = WorkSchedule.builder()
                .user(user)
                .startTime(start)
                .endTime(end)
                .notes(request.getNotes())
                .build();

        WorkSchedule savedSchedule = workScheduleRepository.save(schedule);
        return WorkScheduleResponseDTO.fromWorkSchedule(savedSchedule);
    }

    public List<WorkScheduleResponseDTO> getSchedules(OffsetDateTime start, OffsetDateTime end) {
        return workScheduleRepository.findByStartTimeBetween(start, end)
                .stream()
                .map(WorkScheduleResponseDTO::fromWorkSchedule)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        if (!workScheduleRepository.existsById(scheduleId)) {
            throw new IllegalArgumentException("Không tìm thấy lịch làm việc với ID: " + scheduleId);
        }
        workScheduleRepository.deleteById(scheduleId);
    }

    @Transactional
    public void copyWeek(java.time.LocalDate fromWeekStart, java.time.LocalDate toWeekStart) {
        java.time.OffsetDateTime fromStart = fromWeekStart.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime();
        java.time.OffsetDateTime fromEnd = fromStart.plusDays(7);
        java.util.List<WorkSchedule> source = workScheduleRepository.findByStartTimeBetween(fromStart, fromEnd);
        java.time.Duration delta = java.time.Duration.between(fromStart, toWeekStart.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toOffsetDateTime());
        for (WorkSchedule ws : source) {
            WorkSchedule clone = WorkSchedule.builder()
                    .user(ws.getUser())
                    .startTime(ws.getStartTime().plus(delta))
                    .endTime(ws.getEndTime().plus(delta))
                    .notes(ws.getNotes())
                    .build();
            workScheduleRepository.save(clone);
        }
    }
}
