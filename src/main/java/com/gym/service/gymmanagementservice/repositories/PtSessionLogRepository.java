package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PtSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PtSessionLogRepository extends JpaRepository<PtSessionLog, Long> {
}
