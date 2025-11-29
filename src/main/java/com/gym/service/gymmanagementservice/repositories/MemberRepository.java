package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);
    Optional<Member> findByPhoneNumber(String phoneNumber);
    Optional<Member> findByEmail(String email);
    Optional<Member> findByBarcode(String barcode);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM members WHERE id = :memberId", nativeQuery = true)
    void deleteByIdNative(@org.springframework.data.repository.query.Param("memberId") Long memberId);
    
    @org.springframework.data.jpa.repository.Query(value = "SELECT user_account_id FROM members WHERE id = :memberId LIMIT 1", nativeQuery = true)
    java.util.Optional<Long> findUserIdByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
