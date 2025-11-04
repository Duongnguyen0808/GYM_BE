package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.SaleDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleDetailRepository extends JpaRepository<SaleDetail, Long> {
}