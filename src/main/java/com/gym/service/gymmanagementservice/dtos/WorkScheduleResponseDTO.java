package com.gym.service.gymmanagementservice.dtos;

import com.gym.service.gymmanagementservice.models.WorkSchedule;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class WorkScheduleResponseDTO {
    private Long id;
    private Long userId;
    private String userFullName;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private String notes;

    public static WorkScheduleResponseDTO fromWorkSchedule(WorkSchedule workSchedule) {
        return WorkScheduleResponseDTO.builder()
                .id(workSchedule.getId())
                .userId(workSchedule.getUser().getId())
                .userFullName(workSchedule.getUser().getFullName())
                .startTime(workSchedule.getStartTime())
                .endTime(workSchedule.getEndTime())
                .notes(workSchedule.getNotes())
                .build();
    }
}
