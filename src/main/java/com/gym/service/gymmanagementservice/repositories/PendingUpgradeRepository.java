package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PendingUpgrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingUpgradeRepository extends JpaRepository<PendingUpgrade, Long> {
    Optional<PendingUpgrade> findByTransactionId(Long transactionId);
    void deleteByTransactionId(Long transactionId);
}



















