package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Promotion;
import com.gym.service.gymmanagementservice.models.PromotionTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    List<Promotion> findByTargetTypeAndTargetIdAndIsActiveIsTrueAndStartAtBeforeAndEndAtAfter(
            PromotionTargetType targetType,
            Long targetId,
            OffsetDateTime start,
            OffsetDateTime end
    );
}