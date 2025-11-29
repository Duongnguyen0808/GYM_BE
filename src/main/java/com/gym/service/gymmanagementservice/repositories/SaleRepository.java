package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    java.util.List<Sale> findBySaleDateBetween(java.time.OffsetDateTime start, java.time.OffsetDateTime end);
    long countByUser_Id(Long userId);
    long countByMember_Id(Long memberId);
    java.util.List<Sale> findByMember_Id(Long memberId);
    
    @Modifying
    @Query("UPDATE Sale s SET s.member = null WHERE s.member.id = :memberId")
    void setMemberNullByMemberId(@Param("memberId") Long memberId);
    
    @Modifying
    @Query(value = "DELETE FROM sale_details WHERE sale_id IN (SELECT id FROM sales WHERE member_id = :memberId)", nativeQuery = true)
    void deleteSaleDetailsByMemberId(@Param("memberId") Long memberId);
    
    @Modifying
    @Query(value = "DELETE FROM sales WHERE member_id = :memberId", nativeQuery = true)
    void deleteByMemberId(@Param("memberId") Long memberId);
}
