package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PendingTransactionCleanupService {

    private final TransactionRepository transactionRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final SaleRepository saleRepository;
    private final PendingRenewalRepository pendingRenewalRepository;
    private final PendingUpgradeRepository pendingUpgradeRepository;

    /**
     * Hủy/xóa các transaction PENDING quá 10 phút
     * @return Số lượng transaction đã bị hủy
     */
    @Transactional
    public int cancelExpiredPendingTransactions() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime tenMinutesAgo = now.minusMinutes(10);

        // Tìm tất cả transaction PENDING quá 10 phút
        List<Transaction> expiredTransactions = transactionRepository.findByStatusAndTransactionDateBefore(
                TransactionStatus.PENDING,
                tenMinutesAgo
        );

        if (expiredTransactions.isEmpty()) {
            log.debug("Không có transaction PENDING nào quá 10 phút cần hủy");
            return 0;
        }

        log.info("Tìm thấy {} transaction PENDING quá 10 phút, bắt đầu hủy...", expiredTransactions.size());

        int cancelledCount = 0;

        for (Transaction transaction : expiredTransactions) {
            try {
                // 1. Xử lý MemberPackage nếu có (subscription PENDING)
                if (transaction.getMemberPackage() != null) {
                    MemberPackage memberPackage = transaction.getMemberPackage();
                    if (memberPackage.getStatus() == SubscriptionStatus.PENDING) {
                        log.info("Xóa MemberPackage PENDING (ID: {}) liên kết với transaction {}", 
                                memberPackage.getId(), transaction.getId());
                        memberPackageRepository.delete(memberPackage);
                    }
                }

                // 2. Xử lý Sale nếu có (sale PENDING_PAYMENT)
                if (transaction.getSale() != null) {
                    Sale sale = transaction.getSale();
                    if (sale.getStatus() == SaleStatus.PENDING_PAYMENT) {
                        log.info("Hủy Sale PENDING_PAYMENT (ID: {}) liên kết với transaction {}", 
                                sale.getId(), transaction.getId());
                        sale.setStatus(SaleStatus.CANCELLED);
                        saleRepository.save(sale);
                    }
                }

                // 3. Xóa PendingRenewal nếu có
                pendingRenewalRepository.findByTransactionId(transaction.getId())
                        .ifPresent(pendingRenewal -> {
                            log.info("Xóa PendingRenewal (ID: {}) liên kết với transaction {}", 
                                    pendingRenewal.getId(), transaction.getId());
                            pendingRenewalRepository.delete(pendingRenewal);
                        });

                // 4. Xóa PendingUpgrade nếu có
                pendingUpgradeRepository.findByTransactionId(transaction.getId())
                        .ifPresent(pendingUpgrade -> {
                            log.info("Xóa PendingUpgrade (ID: {}) liên kết với transaction {}", 
                                    pendingUpgrade.getId(), transaction.getId());
                            pendingUpgradeRepository.delete(pendingUpgrade);
                        });

                // 5. Xóa transaction
                transactionRepository.delete(transaction);
                cancelledCount++;

                log.info("Đã hủy transaction PENDING (ID: {}) quá 10 phút", transaction.getId());

            } catch (Exception e) {
                log.error("Lỗi khi hủy transaction ID: " + transaction.getId(), e);
                // Tiếp tục xử lý các transaction khác
            }
        }

        log.info("Hoàn thành: Đã hủy {} transaction PENDING quá 10 phút", cancelledCount);
        return cancelledCount;
    }
}

