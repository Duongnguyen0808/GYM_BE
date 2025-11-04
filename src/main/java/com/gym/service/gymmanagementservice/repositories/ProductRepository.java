package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}