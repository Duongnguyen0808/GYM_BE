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

        WorkSchedule schedule = WorkSchedule.builder()
                .user(user)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
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
}
