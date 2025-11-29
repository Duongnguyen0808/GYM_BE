package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    java.util.List<Transaction> findByTransactionDateBetween(java.time.OffsetDateTime start, java.time.OffsetDateTime end);
    long countByTransactionDateBetweenAndKind(java.time.OffsetDateTime start, java.time.OffsetDateTime end, com.gym.service.gymmanagementservice.models.TransactionKind kind);
    long countByCreatedBy_Id(Long userId);
    java.util.Optional<Transaction> findByMemberPackage_Id(Long memberPackageId);
    
    // Tìm tất cả transaction theo memberPackageId
    java.util.List<Transaction> findAllByMemberPackage_Id(Long memberPackageId);
    
    // Tìm transaction đầu tiên theo memberPackageId, sắp xếp theo ngày giao dịch mới nhất
    java.util.Optional<Transaction> findFirstByMemberPackage_IdOrderByTransactionDateDesc(Long memberPackageId);
    
    // Tìm các transaction PENDING quá thời gian chỉ định
    java.util.List<Transaction> findByStatusAndTransactionDateBefore(
        com.gym.service.gymmanagementservice.models.TransactionStatus status,
        java.time.OffsetDateTime beforeDate
    );
    
    // Tìm transaction theo saleId
    java.util.Optional<Transaction> findBySale_Id(Long saleId);
    
    // Tìm tất cả transaction theo saleId
    java.util.List<Transaction> findAllBySale_Id(Long saleId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("UPDATE Transaction t SET t.sale = null WHERE t.sale.id = :saleId")
    void setSaleNullBySaleId(@org.springframework.data.repository.query.Param("saleId") Long saleId);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM transactions WHERE member_package_id IN (SELECT id FROM member_packages WHERE member_id = :memberId)", nativeQuery = true)
    void deleteByMemberId(@org.springframework.data.repository.query.Param("memberId") Long memberId);
}
