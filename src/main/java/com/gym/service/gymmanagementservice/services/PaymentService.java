package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.config.VNPayConfig;
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
                .paymentMethod(PaymentMethod.BANK_TRANSFER) // Mặc định cho VNPay
                .status(TransactionStatus.PENDING)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(pendingSubscription)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // 3. Tạo URL thanh toán VNPay
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
                .paymentMethod(PaymentMethod.BANK_TRANSFER) // Mặc định cho VNPay
                .status(TransactionStatus.PENDING)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(null)
                .sale(sale)
                .build();
        Transaction savedTransaction = transactionRepository.save(transaction);

        // (Không cần cập nhật Sale status vì nó đã là PENDING_PAYMENT từ SaleService)

        String orderInfo = "Thanh toan hoa don san pham #" + sale.getId();
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

                // Cập nhật ngày bắt đầu/kết thúc dựa trên loại gói
                if(subscription.getGymPackage().getPackageType() == PackageType.GYM_ACCESS || subscription.getGymPackage().getPackageType() == PackageType.PER_VISIT) {
                    OffsetDateTime startDate = OffsetDateTime.now();
                    OffsetDateTime endDate = startDate.plusDays(subscription.getGymPackage().getDurationDays());
                    subscription.setStartDate(startDate);
                    subscription.setEndDate(endDate);
                }
                // (Gói PT không cần ngày bắt đầu/kết thúc)

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