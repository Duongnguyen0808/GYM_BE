package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PtSessionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PtSessionLogRepository extends JpaRepository<PtSessionLog, Long> {
    
    // Lấy tất cả buổi tập của một PT, sắp xếp theo ngày mới nhất
    List<PtSessionLog> findByPtUserId(Long ptUserId);
    
    // Lấy lịch tập với thông tin đầy đủ cho PT
    @Query("SELECT p FROM PtSessionLog p " +
           "LEFT JOIN FETCH p.memberPackage mp " +
           "LEFT JOIN FETCH mp.member m " +
           "LEFT JOIN FETCH mp.gymPackage gp " +
           "WHERE p.ptUser.id = :ptUserId " +
           "ORDER BY p.sessionDate DESC")
    List<PtSessionLog> findByPtUserIdWithDetails(Long ptUserId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM pt_session_logs WHERE member_package_id IN (SELECT id FROM member_packages WHERE member_id = :memberId)", nativeQuery = true)
    void deleteByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
