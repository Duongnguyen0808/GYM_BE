package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface WorkScheduleRepository extends JpaRepository<WorkSchedule, Long> {
    // Tìm tất cả lịch làm việc của một user trong một khoảng thời gian
    List<WorkSchedule> findByUserIdAndStartTimeBetween(Long userId, OffsetDateTime start, OffsetDateTime end);

    // Tìm tất cả lịch làm việc trong một khoảng thời gian
    List<WorkSchedule> findByStartTimeBetween(OffsetDateTime start, OffsetDateTime end);
}
