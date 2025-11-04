package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "packages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GymPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false, unique = true)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false)
    private PackageType packageType; // Phân loại gói

    @Column(name = "duration_days")
    private Integer durationDays; // Dùng cho GYM_ACCESS, PER_VISIT

    @Column(name = "number_of_sessions")
    private Integer numberOfSessions; // Dùng cho PT_SESSION, PER_VISIT

    @Column(name = "start_time_limit")
    private LocalTime startTimeLimit; // Giờ check-in sớm nhất (HH:mm)

    @Column(name = "end_time_limit")
    private LocalTime endTimeLimit; // Giờ check-in trễ nhất (HH:mm)

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}