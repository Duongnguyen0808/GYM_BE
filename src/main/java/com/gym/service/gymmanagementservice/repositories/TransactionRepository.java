package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}