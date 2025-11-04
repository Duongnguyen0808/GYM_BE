package com.gym.service.gymmanagementservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pt_session_logs")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PtSessionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_package_id", nullable = false)
    private MemberPackage memberPackage;

    // PT nào đã dạy buổi này (có thể là PT được gán hoặc PT dạy thay)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pt_user_id", nullable = false)
    private User ptUser;

    @Column(name = "session_date", nullable = false)
    private OffsetDateTime sessionDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
