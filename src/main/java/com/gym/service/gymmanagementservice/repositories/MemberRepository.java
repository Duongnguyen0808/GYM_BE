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
}
