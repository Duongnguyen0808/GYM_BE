package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PendingRenewal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PendingRenewalRepository extends JpaRepository<PendingRenewal, Long> {
    Optional<PendingRenewal> findByTransactionId(Long transactionId);
    void deleteByTransactionId(Long transactionId);
}



















