package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "member_packages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MemberPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne
    @JoinColumn(name = "package_id", nullable = false)
    private GymPackage gymPackage;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    @Column(name = "remaining_sessions")
    private Integer remainingSessions;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_pt_id")
    private User assignedPt; // PT được gán cho gói này

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "allowed_weekdays")
    private String allowedWeekdays; // Ví dụ: "MON,WED,FRI"

    @Enumerated(EnumType.STRING)
    @Column(name = "time_slot")
    private TimeSlot timeSlot; // Khung giờ đã chọn (chỉ dùng cho PT_SESSION)
}
