package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.GymPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository

public interface GymPackageRepository extends JpaRepository<GymPackage, Long> {
    Optional<GymPackage> findByName(String name);
}
