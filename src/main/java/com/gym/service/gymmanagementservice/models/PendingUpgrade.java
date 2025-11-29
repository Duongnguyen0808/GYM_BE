package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pending_upgrades")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingUpgrade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Long subscriptionId; // Gói cũ

    @Column(nullable = false)
    private Long newPackageId; // Gói mới
}



















