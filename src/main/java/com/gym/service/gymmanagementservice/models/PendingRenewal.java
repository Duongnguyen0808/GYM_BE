package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pending_renewals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingRenewal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long packageId;

    private Long assignedPtId; // Nếu là gói PT

    private String allowedWeekdays; // Nếu có giới hạn ngày
}



















