package com.gym.service.gymmanagementservice.tasks;

import com.gym.service.gymmanagementservice.services.PendingTransactionCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingTransactionCleanupTask {

    private final PendingTransactionCleanupService cleanupService;

    /**
     * Run every 2 minutes to check and cancel PENDING transactions older than 10 minutes.
     * Uses fixedDelay: runs 2 minutes after the previous task completes.
     * 120000 milliseconds = 2 minutes
     */
    @Scheduled(fixedDelay = 120000)
    public void cleanupExpiredPendingTransactions() {
        try {
            log.debug("Bắt đầu kiểm tra và hủy các transaction PENDING quá 10 phút...");
            int cancelledCount = cleanupService.cancelExpiredPendingTransactions();
            if (cancelledCount > 0) {
                log.info("Đã tự động hủy {} transaction PENDING quá 10 phút", cancelledCount);
            }
        } catch (Exception e) {
            log.error("Lỗi khi chạy scheduled task cleanup expired pending transactions", e);
        }
    }
}

