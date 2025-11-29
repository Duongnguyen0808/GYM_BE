package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.MemberPackage;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.gym.service.gymmanagementservice.models.SubscriptionStatus;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberPackageRepository extends JpaRepository<MemberPackage, Long> {
    // Tìm tất cả các gói đã đăng ký của một hội viên
    List<MemberPackage> findByMemberId(Long memberId);
    boolean existsByMemberIdAndStatus(Long memberId, SubscriptionStatus status);

    // Tìm gói tập đầu tiên của hội viên theo status, sắp xếp theo ngày hết hạn mới nhất
    Optional<MemberPackage> findFirstByMemberIdAndStatusOrderByEndDateDesc(Long memberId, SubscriptionStatus status);

    // Tìm gói PER_VISIT (ưu tiên gói hết hạn sớm nhất)
    Optional<MemberPackage> findFirstByMemberIdAndStatusAndGymPackage_PackageTypeAndEndDateAfterAndRemainingSessionsGreaterThanOrderByEndDateAsc(
            Long memberId, SubscriptionStatus status, PackageType packageType, OffsetDateTime now, Integer remaining);

    // Tìm gói GYM ACCESS đang hoạt động (dùng cho check-in cổng)
    Optional<MemberPackage> findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
            Long memberId, SubscriptionStatus status, PackageType packageType);

    // Tìm gói PT_SESSION đang hoạt động và còn buổi tập
    Optional<MemberPackage> findFirstByMemberIdAndStatusAndGymPackage_PackageTypeAndRemainingSessionsGreaterThanOrderByEndDateDesc(
            Long memberId, SubscriptionStatus status, PackageType packageType, Integer remaining);

    // Tìm gói đang hoạt động của hội viên, theo ID của GymPackage
    Optional<MemberPackage> findFirstByMemberIdAndStatusAndGymPackage_Id(Long memberId, SubscriptionStatus status, Long gymPackageId);

    boolean existsByGymPackage_Id(Long gymPackageId);

    long countByAssignedPt_Id(Long assignedPtId);

    long countByGymPackage_IdAndStatus(Long gymPackageId, com.gym.service.gymmanagementservice.models.SubscriptionStatus status);

    // Kiểm tra xem đã có MemberPackage nào với cùng PT và timeSlot đang ACTIVE chưa
    boolean existsByAssignedPt_IdAndTimeSlotAndStatus(Long assignedPtId, TimeSlot timeSlot, SubscriptionStatus status);

    // Kiểm tra xem member đã có gói tập cùng GymPackage ID đang ACTIVE chưa
    boolean existsByMemberIdAndGymPackage_IdAndStatus(Long memberId, Long gymPackageId, SubscriptionStatus status);
    
    // Tìm tất cả MemberPackage của một PT, status ACTIVE, có timeSlot và startDate (dùng cho lịch tuần)
    @org.springframework.data.jpa.repository.Query("SELECT mp FROM MemberPackage mp " +
            "JOIN FETCH mp.member m " +
            "JOIN FETCH mp.gymPackage gp " +
            "WHERE mp.assignedPt.id = :ptId " +
            "AND mp.status = :status " +
            "AND gp.packageType = :packageType " +
            "AND mp.timeSlot IS NOT NULL " +
            "AND mp.startDate IS NOT NULL")
    List<MemberPackage> findByAssignedPtIdAndStatusAndGymPackage_PackageTypeAndTimeSlotIsNotNullAndStartDateIsNotNull(
            @org.springframework.data.repository.query.Param("ptId") Long ptId,
            @org.springframework.data.repository.query.Param("status") SubscriptionStatus status,
            @org.springframework.data.repository.query.Param("packageType") PackageType packageType);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM member_packages WHERE member_id = :memberId", nativeQuery = true)
    void deleteAllByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
