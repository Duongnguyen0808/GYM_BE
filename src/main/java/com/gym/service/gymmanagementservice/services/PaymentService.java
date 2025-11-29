package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.config.VNPayConfig;
import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import com.gym.service.gymmanagementservice.utils.VNPayUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepository transactionRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final MemberRepository memberRepository;
    private final GymPackageRepository gymPackageRepository;
    private final SaleRepository saleRepository;
    private final VNPayConfig vnPayConfig;
    private final AuthenticationService authenticationService;
    private final SaleService saleService;
    private final PendingRenewalRepository pendingRenewalRepository;
    private final PendingUpgradeRepository pendingUpgradeRepository;
    private final SubscriptionService subscriptionService;

    @Transactional
    public String createSubscriptionPaymentUrl(HttpServletRequest req, Long memberId, Long packageId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên ID: " + memberId));
        GymPackage gymPackage = gymPackageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập ID: " + packageId));

        // 1. Tạo MemberPackage với trạng thái PENDING
        MemberPackage subscription = MemberPackage.builder()
                .member(member)
                .gymPackage(gymPackage)
                .startDate(null)
                .endDate(null)
                .status(SubscriptionStatus.PENDING)
                .build();
        MemberPackage pendingSubscription = memberPackageRepository.save(subscription);

        // 2. Tạo Transaction với status PENDING
        Transaction transaction = Transaction.builder()
                .amount(gymPackage.getPrice())
                .paymentMethod(PaymentMethod.VN_PAY)
                .status(TransactionStatus.PENDING)
                .kind(TransactionKind.SUBSCRIPTION_NEW)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(pendingSubscription)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 3. Tạo URL thanh toán VNPay
        return VNPayUtil.createPaymentUrl(req, vnPayConfig, savedTransaction.getId(), savedTransaction.getAmount());
    }

    // Hàm tạo yêu cầu thanh toán cho GIA HẠN GÓI TẬP
    @Transactional
    public String createRenewPaymentUrl(HttpServletRequest req, Long memberId, Long packageId, Long assignedPtId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên ID: " + memberId));
        GymPackage gymPackage = gymPackageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập ID: " + packageId));

        // Tạo Transaction PENDING cho gia hạn
        Transaction transaction = Transaction.builder()
                .amount(gymPackage.getPrice())
                .paymentMethod(PaymentMethod.VN_PAY)
                .status(TransactionStatus.PENDING)
                .kind(TransactionKind.SUBSCRIPTION_RENEW)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(null) // Chưa có package, sẽ tạo sau khi thanh toán thành công
                .sale(null)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Lưu thông tin gia hạn vào bảng pending
        PendingRenewal pendingRenewal = PendingRenewal.builder()
                .transactionId(savedTransaction.getId())
                .memberId(memberId)
                .packageId(packageId)
                .assignedPtId(assignedPtId)
                .build();
        pendingRenewalRepository.save(pendingRenewal);

        return VNPayUtil.createPaymentUrl(req, vnPayConfig, savedTransaction.getId(), savedTransaction.getAmount());
    }

    // Hàm tạo yêu cầu thanh toán cho HÓA ĐƠN BÁN HÀNG (SALE)
    @Transactional
    public String createSalePaymentUrl(HttpServletRequest req, Long saleId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hóa đơn bán hàng ID: " + saleId));

        // Kiểm tra Sale phải ở trạng thái PENDING_PAYMENT
        if (sale.getStatus() != SaleStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Hóa đơn này không ở trạng thái chờ thanh toán.");
        }

        Transaction transaction = Transaction.builder()
                .amount(sale.getTotalAmount())
                .paymentMethod(PaymentMethod.VN_PAY)
                .status(TransactionStatus.PENDING)
                .kind(TransactionKind.SALE)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(null)
                .sale(sale)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // (Không cần cập nhật Sale status vì nó đã là PENDING_PAYMENT từ SaleService)

        return VNPayUtil.createPaymentUrl(req, vnPayConfig, savedTransaction.getId(), savedTransaction.getAmount());
    }

    // Hàm tạo yêu cầu thanh toán cho NÂNG CẤP GÓI TẬP
    @Transactional
    public String createUpgradePaymentUrl(HttpServletRequest req, Long subscriptionId, Long newPackageId) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        
        // Tính toán số tiền phải trả (tương tự upgradeSubscription nhưng chưa tạo gói mới)
        MemberPackage current = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ nâng cấp từ gói đang hoạt động.");
        }

        GymPackage currentPkg = current.getGymPackage();
        GymPackage newPkg = gymPackageRepository.findById(newPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + newPackageId));

        if (newPkg.getPrice().compareTo(currentPkg.getPrice()) <= 0) {
            throw new IllegalStateException("Gói mới phải có giá cao hơn gói hiện tại để nâng cấp.");
        }

        // Tính giá trị hoàn lại và số tiền phải trả
        OffsetDateTime now = OffsetDateTime.now();
        java.math.BigDecimal refundValue = java.math.BigDecimal.ZERO;

        if (currentPkg.getPackageType() == PackageType.PT_SESSION) {
            Integer totalSessions = currentPkg.getNumberOfSessions() != null ? currentPkg.getNumberOfSessions() : 0;
            Integer remainingSessions = current.getRemainingSessions() != null ? current.getRemainingSessions() : 0;
            
            if (totalSessions <= 0 || remainingSessions <= 0) {
                throw new IllegalStateException("Gói PT không hợp lệ hoặc đã hết buổi.");
            }

            java.math.BigDecimal perSessionValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalSessions), 2, java.math.RoundingMode.HALF_UP);
            
            // Chỉ tính hoàn lại cho số buổi ban đầu (không tính buổi gia hạn)
            int sessionsToRefund = Math.min(remainingSessions, totalSessions);
            refundValue = perSessionValue.multiply(java.math.BigDecimal.valueOf(sessionsToRefund));
            
            // Đảm bảo giá trị hoàn lại không vượt quá giá đã thanh toán
            if (refundValue.compareTo(currentPkg.getPrice()) > 0) {
                refundValue = currentPkg.getPrice();
            }
            
        } else if (currentPkg.getPackageType() == PackageType.GYM_ACCESS) {
            // Gói GYM_ACCESS: Tính hoàn lại theo ngày (từ tổng số ngày thực tế)
            if (current.getEndDate() == null) {
                throw new IllegalStateException("Gói tập không có ngày kết thúc.");
            }
            
            if (current.getStartDate() == null) {
                throw new IllegalStateException("Gói tập không có ngày bắt đầu.");
            }
            
            // Tính tổng số ngày thực tế từ startDate đến endDate
            long totalDays = java.time.temporal.ChronoUnit.DAYS.between(
                current.getStartDate(), current.getEndDate());
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now, current.getEndDate());
            
            if (remainingDays <= 0) {
                throw new IllegalStateException("Gói tập đã hết hạn.");
            }

            if (totalDays <= 0) {
                throw new IllegalStateException("Gói tập không hợp lệ (không có thời hạn).");
            }

            java.math.BigDecimal dailyValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP);
            refundValue = dailyValue.multiply(java.math.BigDecimal.valueOf(remainingDays));
        } else if (currentPkg.getPackageType() == PackageType.PER_VISIT) {
            if (current.getEndDate() == null) {
                throw new IllegalStateException("Gói tập không có ngày kết thúc.");
            }
            
            long totalDays = currentPkg.getDurationDays();
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now, current.getEndDate());
            
            if (remainingDays <= 0 || totalDays <= 0) {
                throw new IllegalStateException("Gói tập đã hết hạn hoặc không hợp lệ.");
            }

            java.math.BigDecimal dailyValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP);
            refundValue = dailyValue.multiply(java.math.BigDecimal.valueOf(remainingDays));
        }

        // Tính giá gói mới (có thể có giảm giá)
        Member member = current.getMember();
        java.math.BigDecimal newPackagePrice = subscriptionService.calculateFinalPrice(member, newPkg);
        java.math.BigDecimal amountToPay = newPackagePrice.subtract(refundValue);
        
        if (amountToPay.compareTo(java.math.BigDecimal.ZERO) < 0) {
            amountToPay = java.math.BigDecimal.ZERO;
        }

        // Tạo Transaction PENDING
        Transaction transaction = Transaction.builder()
                .amount(amountToPay)
                .paymentMethod(PaymentMethod.VN_PAY)
                .status(TransactionStatus.PENDING)
                .kind(TransactionKind.SUBSCRIPTION_UPGRADE)
                .transactionDate(now)
                .createdBy(currentUser)
                .memberPackage(current) // Lưu gói cũ để xử lý sau
                .sale(null)
                .build();
        
        Transaction savedTransaction = transactionRepository.save(transaction);

        // Lưu thông tin upgrade vào bảng pending
        PendingUpgrade pendingUpgrade = PendingUpgrade.builder()
                .transactionId(savedTransaction.getId())
                .subscriptionId(subscriptionId)
                .newPackageId(newPackageId)
                .build();
        pendingUpgradeRepository.save(pendingUpgrade);

        return VNPayUtil.createPaymentUrl(req, vnPayConfig, savedTransaction.getId(), savedTransaction.getAmount());
    }

    // Hàm xử lý IPN từ VNPay (Cập nhật cho cả Subscription và Sale)
    @Transactional
    public boolean processVNPayIPN(Map<String, String[]> params) {
        log.info("Received VNPay IPN: {}", params);

        if (!VNPayUtil.verifyIPNResponse(params, vnPayConfig.getHashSecret())) {
            log.error("VNPay IPN checksum failed!");
            return false;
        }

        String transactionIdStr = params.get("vnp_TxnRef")[0];
        String responseCode = params.get("vnp_ResponseCode")[0];

        Long transactionId = Long.parseLong(transactionIdStr);
        Optional<Transaction> transactionOpt = transactionRepository.findById(transactionId);

        if (transactionOpt.isEmpty()) {
            log.error("Transaction not found for ID received from VNPay IPN: {}", transactionId);
            return false;
        }

        Transaction transaction = transactionOpt.get();

        if (transaction.getStatus() != TransactionStatus.PENDING) {
            log.warn("Transaction {} already processed with status: {}.", transactionId, transaction.getStatus());
            return true;
        }

        if ("00".equals(responseCode)) {
            // Thanh toán thành công
            transaction.setStatus(TransactionStatus.COMPLETED);
            log.info("VNPay Transaction {} completed successfully.", transactionId);

            // Xử lý Gói tập
            MemberPackage subscription = transaction.getMemberPackage();
            if (subscription != null && subscription.getStatus() == SubscriptionStatus.PENDING) {
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                OffsetDateTime startDate = OffsetDateTime.now();

                GymPackage gymPackage = subscription.getGymPackage();
                if (gymPackage != null) {
                    PackageType packageType = gymPackage.getPackageType();

                    if (packageType == PackageType.GYM_ACCESS) {
                        OffsetDateTime endDate;
                        if (gymPackage.getDurationMonths() != null && gymPackage.getDurationMonths() > 0) {
                            endDate = startDate.plusMonths(gymPackage.getDurationMonths());
                        } else if (gymPackage.getDurationDays() != null && gymPackage.getDurationDays() > 0) {
                            endDate = startDate.plusDays(gymPackage.getDurationDays());
                        } else {
                            throw new IllegalStateException("Gói GYM_ACCESS không có thời hạn hợp lệ để kích hoạt.");
                        }
                        subscription.setStartDate(startDate);
                        subscription.setEndDate(endDate);
                    } else if (packageType == PackageType.PER_VISIT) {
                        Integer durationDays = gymPackage.getDurationDays();
                        if (durationDays == null || durationDays <= 0) {
                            throw new IllegalStateException("Gói PER_VISIT phải có durationDays hợp lệ.");
                        }
                        OffsetDateTime endDate = startDate.plusDays(durationDays);
                        subscription.setStartDate(startDate);
                        subscription.setEndDate(endDate);
                    } else if (packageType == PackageType.PT_SESSION) {
                        subscription.setStartDate(startDate);
                        if (gymPackage.getDurationMonths() != null && gymPackage.getDurationMonths() > 0) {
                            subscription.setEndDate(startDate.plusMonths(gymPackage.getDurationMonths()));
                        } else {
                            subscription.setEndDate(null);
                        }
                    }
                }

                memberPackageRepository.save(subscription);
                log.info("Activated MemberPackage {} for Transaction {}.", subscription.getId(), transactionId);
            }

            // Xử lý Hóa đơn bán hàng
            Sale sale = transaction.getSale();
            if (sale != null && sale.getStatus() == SaleStatus.PENDING_PAYMENT) {
                sale.setStatus(SaleStatus.PAID);
                saleRepository.save(sale);
                log.info("Updated Sale {} to PAID for Transaction {}.", sale.getId(), transactionId);

                // *** MỚI: TRỪ TỒN KHO KHI THANH TOÁN THÀNH CÔNG ***
                try {
                    saleService.deductStockForSale(sale);
                    log.info("Deducted stock for Sale ID: {}", sale.getId());
                } catch (IllegalStateException e) {
                    log.error("!!! CRITICAL: Payment Success but FAILED to deduct stock for Sale ID: {}", sale.getId(), e);
                    // (Bạn có thể thêm logic thông báo cho admin ở đây)
                    // Vẫn trả về 'true' vì tiền đã nhận
                }
            }

            // Xử lý Gia hạn gói tập
            if (transaction.getKind() == TransactionKind.SUBSCRIPTION_RENEW) {
                Optional<PendingRenewal> pendingRenewalOpt = pendingRenewalRepository.findByTransactionId(transactionId);
                if (pendingRenewalOpt.isPresent()) {
                    PendingRenewal pendingRenewal = pendingRenewalOpt.get();
                    try {
                        // Tạo SubscriptionRequestDTO để gọi renewSubscription
                        SubscriptionRequestDTO renewRequest = new SubscriptionRequestDTO();
                        renewRequest.setMemberId(pendingRenewal.getMemberId());
                        renewRequest.setPackageId(pendingRenewal.getPackageId());
                        renewRequest.setPaymentMethod(PaymentMethod.VN_PAY);
                        renewRequest.setAssignedPtId(pendingRenewal.getAssignedPtId());
                        
                        subscriptionService.renewSubscription(renewRequest);
                        log.info("Successfully renewed subscription for member {} with Transaction {}.", 
                                pendingRenewal.getMemberId(), transactionId);
                        
                        // Xóa pending renewal sau khi xử lý xong
                        pendingRenewalRepository.delete(pendingRenewal);
                    } catch (Exception e) {
                        log.error("Failed to process renew for Transaction {}: {}", transactionId, e.getMessage());
                    }
                } else {
                    log.warn("PendingRenewal not found for RENEW Transaction {}.", transactionId);
                }
            }

            // Xử lý Nâng cấp gói tập
            if (transaction.getKind() == TransactionKind.SUBSCRIPTION_UPGRADE) {
                Optional<PendingUpgrade> pendingUpgradeOpt = pendingUpgradeRepository.findByTransactionId(transactionId);
                if (pendingUpgradeOpt.isPresent()) {
                    PendingUpgrade pendingUpgrade = pendingUpgradeOpt.get();
                    try {
                        subscriptionService.upgradeSubscription(
                                pendingUpgrade.getSubscriptionId(),
                                pendingUpgrade.getNewPackageId(),
                                PaymentMethod.VN_PAY
                        );
                        log.info("Successfully upgraded subscription {} to package {} with Transaction {}.", 
                                pendingUpgrade.getSubscriptionId(), pendingUpgrade.getNewPackageId(), transactionId);
                        
                        // Xóa pending upgrade sau khi xử lý xong
                        pendingUpgradeRepository.delete(pendingUpgrade);
                    } catch (Exception e) {
                        log.error("Failed to process upgrade for Transaction {}: {}", transactionId, e.getMessage());
                    }
                } else {
                    log.warn("PendingUpgrade not found for UPGRADE Transaction {}.", transactionId);
                }
            }

        } else {
            // Thanh toán thất bại
            transaction.setStatus(TransactionStatus.FAILED);
            log.error("VNPay Transaction {} failed with code: {}", transactionId, responseCode);

            // Xử lý Gói tập
            MemberPackage subscription = transaction.getMemberPackage();
            if (subscription != null && subscription.getStatus() == SubscriptionStatus.PENDING) {
                subscription.setStatus(SubscriptionStatus.CANCELLED);
                memberPackageRepository.save(subscription);
                log.warn("Cancelled MemberPackage {} due to failed Transaction {}.", subscription.getId(), transactionId);
            }

            // Xử lý Hóa đơn bán hàng
            Sale sale = transaction.getSale();
            if (sale != null && sale.getStatus() == SaleStatus.PENDING_PAYMENT) {
                sale.setStatus(SaleStatus.PAYMENT_FAILED);
                saleRepository.save(sale);
                log.warn("Updated Sale {} to PAYMENT_FAILED for Transaction {}.", sale.getId(), transactionId);
                // Không cần hoàn trả tồn kho, vì chúng ta chưa bao giờ trừ nó
            }
        }

        transactionRepository.save(transaction);
        return "00".equals(responseCode);
    }
}