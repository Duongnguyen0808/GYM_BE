package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.StaffAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

@Repository
public interface StaffAttendanceRepository extends JpaRepository<StaffAttendance, Long> {
    Optional<StaffAttendance> findTopByUser_IdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long userId);
    List<StaffAttendance> findByUser_IdAndCheckInTimeBetween(Long userId, OffsetDateTime start, OffsetDateTime end);
    long countByUser_Id(Long userId);
}
