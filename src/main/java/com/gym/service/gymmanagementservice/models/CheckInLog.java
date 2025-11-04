package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "check_in_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CheckInLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "check_in_time", nullable = false)
    private OffsetDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CheckInStatus status;

    @Column(name = "message")
    private String message;
}
