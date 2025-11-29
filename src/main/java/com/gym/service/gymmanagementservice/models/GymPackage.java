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
    private Integer durationDays; // Dùng cho PER_VISIT hoặc backward compatibility cho GYM_ACCESS

    @Column(name = "duration_months")
    private Integer durationMonths; // Dùng cho PT_SESSION và GYM_ACCESS - thời hạn tính theo tháng (1, 2, 3, 6, 12, 24...)

    @Column(name = "number_of_sessions")
    private Integer numberOfSessions; // Dùng cho PT_SESSION, PER_VISIT

    @Column(name = "sessions_per_week")
    private Integer sessionsPerWeek; // Quy định số buổi/tuần (tuỳ chọn)

    @Column(name = "is_unlimited")
    private Boolean unlimited; // Không giới hạn lượt/buổi (tuỳ chọn)

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

    @Column(name = "hinh_anh", length = 1024)
    private String hinhAnh;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_pt_id")
    private User assignedPt; // PT được gán cho gói PT (chỉ dùng cho PT_SESSION)

    @Column(name = "allowed_weekdays", length = 100)
    private String allowedWeekdays; // Các thứ trong tuần cho phép tập, định dạng CSV: "MON,WED,FRI" (chỉ dùng cho PT_SESSION)

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
