package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckInLogRepository extends JpaRepository<CheckInLog, Long> {
}