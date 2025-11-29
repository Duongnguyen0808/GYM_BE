package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.CheckInLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheckInLogRepository extends JpaRepository<CheckInLog, Long> {
    java.util.Optional<CheckInLog> findTopByMemberIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long memberId);
    java.util.Optional<CheckInLog> findTopByMemberPackageIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(Long memberPackageId);

    long countByCheckInTimeBetween(java.time.OffsetDateTime start, java.time.OffsetDateTime end);
    long countByCheckInTimeBetweenAndStatus(java.time.OffsetDateTime start, java.time.OffsetDateTime end, com.gym.service.gymmanagementservice.models.CheckInStatus status);
    
    // Query methods for filtering
    java.util.List<CheckInLog> findByMemberIdOrderByCheckInTimeDesc(Long memberId, org.springframework.data.domain.Pageable pageable);
    java.util.List<CheckInLog> findByMemberPackageIdOrderByCheckInTimeDesc(Long packageId, org.springframework.data.domain.Pageable pageable);
    java.util.List<CheckInLog> findByCheckInTimeBetweenOrderByCheckInTimeDesc(
            java.time.OffsetDateTime start, java.time.OffsetDateTime end, org.springframework.data.domain.Pageable pageable);
    java.util.List<CheckInLog> findByMemberIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long memberId, java.time.OffsetDateTime start, java.time.OffsetDateTime end, org.springframework.data.domain.Pageable pageable);
    java.util.List<CheckInLog> findByMemberPackageIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long packageId, java.time.OffsetDateTime start, java.time.OffsetDateTime end, org.springframework.data.domain.Pageable pageable);
    java.util.List<CheckInLog> findByMemberIdAndMemberPackageIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
            Long memberId, Long packageId, java.time.OffsetDateTime start, java.time.OffsetDateTime end, org.springframework.data.domain.Pageable pageable);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM check_in_logs WHERE member_id = :memberId", nativeQuery = true)
    void deleteAllByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
