package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.FreezeRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionRequestDTO;
import com.gym.service.gymmanagementservice.dtos.SubscriptionResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final MemberRepository memberRepository;
    private final GymPackageRepository gymPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuthenticationService authenticationService;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final PromotionService promotionService;

    /**
     * Tính giá chỉ áp dụng Promotion/Voucher từ database:
     * - Tìm promotion đang active cho gói này
     * - Áp dụng promotion trên giá gốc
     * 
     * Công thức: Giá cuối = Giá gốc × (1 - %Promotion)
     * 
     * Ví dụ:
     * - Gói 20,000,000đ, có promotion -20%:
     *   20,000,000 × 0.80 = 16,000,000đ
     */
    public java.math.BigDecimal calculateFinalPrice(Member member, GymPackage gymPackage) {
        java.math.BigDecimal basePrice = gymPackage.getPrice();
        
        // Tìm promotion đang active cho gói này
        java.util.Optional<Promotion> activePromotion = promotionService.getActivePromotionForPackage(gymPackage.getId());
        
        if (activePromotion.isPresent()) {
            Promotion promo = activePromotion.get();
            java.math.BigDecimal discountPercent = promo.getDiscountPercent();
            
            // Áp dụng promotion trên giá gốc
            java.math.BigDecimal promotionMultiplier = java.math.BigDecimal.ONE
                    .subtract(discountPercent.divide(new java.math.BigDecimal("100")));
            
            return basePrice.multiply(promotionMultiplier);
        }
        
        return basePrice;
    }

    /**
     * Chỉ áp dụng Khuyến mãi/Voucher, KHÔNG áp dụng giảm tự động.
     */
    private java.math.BigDecimal calculateFinalPricePromoOnly(Member member, GymPackage gymPackage) {
        java.math.BigDecimal basePrice = gymPackage.getPrice();
        java.util.Optional<Promotion> activePromotion = promotionService.getActivePromotionForPackage(gymPackage.getId());
        if (activePromotion.isPresent()) {
            java.math.BigDecimal discountPercent = activePromotion.get().getDiscountPercent();
            java.math.BigDecimal promotionMultiplier = java.math.BigDecimal.ONE
                    .subtract(discountPercent.divide(new java.math.BigDecimal("100")));
            return basePrice.multiply(promotionMultiplier);
        }
        return basePrice;
    }

    public java.math.BigDecimal estimateSubscriptionPrice(Long memberId, Long packageId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));
        GymPackage gymPackage = gymPackageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + packageId));
        return calculateFinalPrice(member, gymPackage);
    }

    public java.util.Map<String, Object> estimateSubscriptionBreakdown(Long memberId, Long packageId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));
        GymPackage gymPackage = gymPackageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + packageId));

        java.math.BigDecimal base = gymPackage.getPrice();
        java.util.Optional<Promotion> promoOpt = promotionService.getActivePromotionForPackage(gymPackage.getId());
        java.math.BigDecimal promoPercent = promoOpt.map(Promotion::getDiscountPercent).orElse(java.math.BigDecimal.ZERO);

        // Áp dụng FULL giảm giá (tự động + promotion)
        java.math.BigDecimal finalPrice = calculateFinalPrice(member, gymPackage);

        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("basePrice", base);
        map.put("promoPercent", promoPercent);
        map.put("finalPrice", finalPrice);
        return map;
    }

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage gymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        User currentUser = authenticationService.getCurrentAuthenticatedUser(); // Lấy user đang thực hiện

        // === KIỂM TRA TRÙNG GÓI ===
        // Ngăn mua lại gói tập đang ACTIVE (trừ PER_VISIT có thể mua song song)
        if (gymPackage.getPackageType() != PackageType.PER_VISIT) {
            boolean hasDuplicateActivePackage = memberPackageRepository.existsByMemberIdAndGymPackage_IdAndStatus(
                    member.getId(), gymPackage.getId(), SubscriptionStatus.ACTIVE);
            
            if (hasDuplicateActivePackage) {
                throw new IllegalStateException("Hội viên này đã có gói tập \"" + gymPackage.getName() + 
                        "\" đang hoạt động. Vui lòng sử dụng chức năng Gia hạn hoặc đợi gói hiện tại hết hạn.");
            }
        }

        MemberPackage.MemberPackageBuilder subscriptionBuilder = MemberPackage.builder()
                .member(member)
                .gymPackage(gymPackage)
                .status(SubscriptionStatus.ACTIVE); // Mặc định là ACTIVE khi tạo mới

        // === LOGIC PHÂN LOẠI ===

        if (gymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            // 1. XỬ LÝ GÓI GYM ACCESS
            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate;
            
            // Ưu tiên durationMonths, fallback về durationDays (backward compatibility)
            if (gymPackage.getDurationMonths() != null && gymPackage.getDurationMonths() > 0) {
                // Tính từ ngày mua đến đúng số tháng sau (ví dụ: mua 15/11, 3 tháng → hết hạn 15/02)
                endDate = startDate.plusMonths(gymPackage.getDurationMonths());
            } else if (gymPackage.getDurationDays() != null && gymPackage.getDurationDays() > 0) {
                // Backward compatibility: dùng durationDays nếu không có durationMonths
                endDate = startDate.plusDays(gymPackage.getDurationDays());
            } else {
                throw new IllegalStateException("Gói GYM_ACCESS phải có durationMonths hoặc durationDays.");
            }
            
            subscriptionBuilder.startDate(startDate).endDate(endDate);

        } else if (gymPackage.getPackageType() == PackageType.PT_SESSION) {
            // 2. XỬ LÝ GÓI PT
            subscriptionBuilder.remainingSessions(gymPackage.getNumberOfSessions());
            
            // Tính thời hạn từ durationMonths
            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate;
            if (gymPackage.getDurationMonths() != null && gymPackage.getDurationMonths() > 0) {
                // Tính từ ngày mua đến đúng số tháng sau (ví dụ: mua 15/11, 1 tháng → hết hạn 15/12)
                endDate = startDate.plusMonths(gymPackage.getDurationMonths());
            } else {
                // Fallback: nếu không có durationMonths, không set endDate (null)
                endDate = null;
            }
            subscriptionBuilder.startDate(startDate).endDate(endDate);
            
            // Lấy PT từ GymPackage (ưu tiên) hoặc từ request
            User assignedPt = null;
            if (gymPackage.getAssignedPt() != null) {
                // Nếu gói đã có PT được gán, tự động dùng PT đó, không cần assignedPtId trong request
                assignedPt = gymPackage.getAssignedPt();
            } else if (request.getAssignedPtId() != null) {
                // Chỉ dùng assignedPtId từ request nếu gói chưa có PT
                assignedPt = userRepository.findById(request.getAssignedPtId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                if (assignedPt.getRole() != Role.PT) {
                    throw new IllegalArgumentException("Người dùng (ID: " + assignedPt.getId() + ") không phải là PT.");
                }
            } else {
                throw new IllegalArgumentException("Gói PT phải có PT được gán. Vui lòng gán PT khi tạo gói hoặc khi đăng ký.");
            }
            
            subscriptionBuilder.assignedPt(assignedPt);
            
            // Validate timeSlot là bắt buộc cho PT_SESSION
            if (request.getTimeSlot() == null) {
                throw new IllegalArgumentException("Gói PT phải chọn khung giờ: MORNING, AFTERNOON_1, AFTERNOON_2, hoặc EVENING.");
            }
            
            // Kiểm tra xem khung giờ đã được đặt chưa
            boolean timeSlotTaken = memberPackageRepository.existsByAssignedPt_IdAndTimeSlotAndStatus(
                    assignedPt.getId(), request.getTimeSlot(), SubscriptionStatus.ACTIVE);
            
            if (timeSlotTaken) {
                throw new IllegalStateException("Khung giờ " + request.getTimeSlot().getDisplayName() + 
                        " đã được đặt bởi hội viên khác. Vui lòng chọn khung giờ khác.");
            }
            
            subscriptionBuilder.timeSlot(request.getTimeSlot());

        } else if (gymPackage.getPackageType() == PackageType.PER_VISIT) {
            // 3. MỚI: XỬ LÝ GÓI PER_VISIT
            // Gói theo lượt luôn được phép mua song song
            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate = startDate.plusDays(gymPackage.getDurationDays());

            subscriptionBuilder.startDate(startDate)
                    .endDate(endDate)
                    .remainingSessions(gymPackage.getNumberOfSessions());
        }

        // Lưu gói đăng ký (MemberPackage)
        MemberPackage toSave = subscriptionBuilder.build();
        
        // Xử lý allowedWeekdays: ưu tiên từ request, nếu không có thì lấy từ gói
        if (request.getAllowedWeekdays() != null && !request.getAllowedWeekdays().isBlank()) {
            toSave.setAllowedWeekdays(request.getAllowedWeekdays());
        } else if (gymPackage.getAllowedWeekdays() != null && !gymPackage.getAllowedWeekdays().isBlank()) {
            // Nếu request không có, lấy từ gói (nếu gói có)
            toSave.setAllowedWeekdays(gymPackage.getAllowedWeekdays());
        }
        // timeSlot đã được set trong subscriptionBuilder cho PT_SESSION
        MemberPackage savedSubscription = memberPackageRepository.save(toSave);

        // === TẠO GIAO DỊCH (TRANSACTION) ===
        // Tính giá cuối cùng (đã gồm Tầng 1 tự động + Tầng 2 khuyến mại)
        java.math.BigDecimal amount = calculateFinalPrice(member, gymPackage);

        Transaction transaction = Transaction.builder()
                .amount(amount)
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .kind(TransactionKind.SUBSCRIPTION_NEW)
                .transactionDate(OffsetDateTime.now())
                .createdBy(currentUser)
                .memberPackage(savedSubscription)
                .sale(null)
                .build();

        transactionRepository.save(transaction);

        return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
    }

    public List<SubscriptionResponseDTO> getSubscriptionsByMemberId(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId);
        }

        return memberPackageRepository.findByMemberId(memberId).stream()
                .map(SubscriptionResponseDTO::fromMemberPackage)
                .collect(Collectors.toList());
    }

    public List<SubscriptionResponseDTO> searchSubscriptions(Long memberId,
                                                             String status,
                                                             String packageType,
                                                             OffsetDateTime startFrom,
                                                             OffsetDateTime endTo,
                                                             Long assignedPtId) {
        return memberPackageRepository.findAll().stream()
                .filter(mp -> memberId == null || (mp.getMember() != null && mp.getMember().getId().equals(memberId)))
                .filter(mp -> status == null || mp.getStatus().name().equalsIgnoreCase(status))
                .filter(mp -> packageType == null || (mp.getGymPackage() != null && mp.getGymPackage().getPackageType().name().equalsIgnoreCase(packageType)))
                .filter(mp -> startFrom == null || (mp.getStartDate() != null && !mp.getStartDate().isBefore(startFrom)))
                .filter(mp -> endTo == null || (mp.getEndDate() != null && !mp.getEndDate().isAfter(endTo)))
                .filter(mp -> assignedPtId == null || (mp.getAssignedPt() != null && mp.getAssignedPt().getId().equals(assignedPtId)))
                .map(SubscriptionResponseDTO::fromMemberPackage)
                .collect(Collectors.toList());
    }

    // Gia hạn gói tập
    @Transactional
    public SubscriptionResponseDTO renewSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage newGymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        // TRƯỜNG HỢP 1: Gia hạn gói PT (CỘNG DỒN SỐ BUỔI)
        if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {

            // Tìm xem hội viên có gói PT CÙNG LOẠI (cùng gymPackage.id) nào đang ACTIVE không
            Optional<MemberPackage> existingActivePtPackageOpt = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_Id(
                            member.getId(),
                            SubscriptionStatus.ACTIVE,
                            newGymPackage.getId()
                    );

            if (existingActivePtPackageOpt.isPresent()) {
                // Nếu có -> Cộng dồn số buổi
                MemberPackage packageToRenew = existingActivePtPackageOpt.get();
                int currentSessions = packageToRenew.getRemainingSessions() != null ? packageToRenew.getRemainingSessions() : 0;
                int newSessions = newGymPackage.getNumberOfSessions() != null ? newGymPackage.getNumberOfSessions() : 0;

                packageToRenew.setRemainingSessions(currentSessions + newSessions);

                // Cập nhật PT được gán: ưu tiên PT từ gói mới, sau đó mới đến request
                User ptToAssign = null;
                if (newGymPackage.getAssignedPt() != null) {
                    ptToAssign = newGymPackage.getAssignedPt();
                } else if (request.getAssignedPtId() != null) {
                    ptToAssign = userRepository.findById(request.getAssignedPtId())
                            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                    if (ptToAssign.getRole() != Role.PT) {
                        throw new IllegalArgumentException("Người dùng (ID: " + ptToAssign.getId() + ") không phải là PT.");
                    }
                }
                // Nếu có PT mới, cập nhật; nếu không, giữ nguyên PT hiện tại
                if (ptToAssign != null) {
                    packageToRenew.setAssignedPt(ptToAssign);
                }

                // Cập nhật thời hạn: nếu gói mới có durationMonths, tính lại endDate
                if (newGymPackage.getDurationMonths() != null && newGymPackage.getDurationMonths() > 0) {
                    OffsetDateTime currentEndDate = packageToRenew.getEndDate();
                    if (currentEndDate == null) {
                        // Nếu chưa có endDate, tính từ ngày hiện tại
                        currentEndDate = OffsetDateTime.now();
                    }
                    // Gia hạn thêm số tháng từ gói mới
                    packageToRenew.setEndDate(currentEndDate.plusMonths(newGymPackage.getDurationMonths()));
                }

                // Nếu gói đã hết hạn (remainingSessions = 0) thì kích hoạt lại
                if (packageToRenew.getStatus() == SubscriptionStatus.EXPIRED) {
                    packageToRenew.setStatus(SubscriptionStatus.ACTIVE);
                }

                MemberPackage savedSubscription = memberPackageRepository.save(packageToRenew);

                // Áp dụng FULL giảm giá (tự động + promotion) cho gia hạn
                java.math.BigDecimal amount = calculateFinalPrice(member, newGymPackage);
                User currentUser = authenticationService.getCurrentAuthenticatedUser();
                Transaction tx = Transaction.builder()
                        .amount(amount)
                        .paymentMethod(request.getPaymentMethod())
                        .status(TransactionStatus.COMPLETED)
                        .kind(TransactionKind.SUBSCRIPTION_RENEW)
                        .transactionDate(java.time.OffsetDateTime.now())
                        .createdBy(currentUser)
                        .memberPackage(savedSubscription)
                        .sale(null)
                        .build();
                transactionRepository.save(tx);

                return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
            }

            // Nếu không có gói PT cùng loại đang active -> Rơi xuống logic "Tạo mới" bên dưới
        }

        // TRƯỜNG HỢP 2: Gia hạn gói GYM_ACCESS (NỐI TIẾP THỜI GIAN)
        // Hoặc mua mới gói PT (khi chưa có gói PT cùng loại)

        MemberPackage.MemberPackageBuilder newSubscriptionBuilder = MemberPackage.builder()
                .member(member)
                .gymPackage(newGymPackage)
                .status(SubscriptionStatus.ACTIVE);

        if (newGymPackage.getPackageType() == PackageType.GYM_ACCESS) {

            // Tìm gói GYM ACCESS đang ACTIVE gần nhất để tính ngày bắt đầu cho gói mới
            MemberPackage lastActivePackage = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                            member.getId(),
                            SubscriptionStatus.ACTIVE,
                            PackageType.GYM_ACCESS
                    )
                    .orElseThrow(() -> new IllegalStateException("Hội viên không có gói thời hạn nào đang hoạt động để gia hạn."));

            // Ngày bắt đầu của gói mới là ngày kết thúc của gói cũ
            OffsetDateTime newStartDate = lastActivePackage.getEndDate();
            OffsetDateTime newEndDate;
            
            // Ưu tiên durationMonths, fallback về durationDays (backward compatibility)
            if (newGymPackage.getDurationMonths() != null && newGymPackage.getDurationMonths() > 0) {
                newEndDate = newStartDate.plusMonths(newGymPackage.getDurationMonths());
            } else if (newGymPackage.getDurationDays() != null && newGymPackage.getDurationDays() > 0) {
                newEndDate = newStartDate.plusDays(newGymPackage.getDurationDays());
            } else {
                throw new IllegalStateException("Gói GYM_ACCESS phải có durationMonths hoặc durationDays.");
            }

            newSubscriptionBuilder.startDate(newStartDate).endDate(newEndDate);

        } else if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {
            // Trường hợp này là mua mới gói PT (vì không tìm thấy gói cùng loại ở trên)
            // Logic giống createSubscription
            newSubscriptionBuilder.remainingSessions(newGymPackage.getNumberOfSessions());

            // Lấy PT từ GymPackage (ưu tiên) hoặc từ request
            User assignedPt = null;
            if (newGymPackage.getAssignedPt() != null) {
                // Nếu gói đã có PT được gán, tự động dùng PT đó
                assignedPt = newGymPackage.getAssignedPt();
            } else if (request.getAssignedPtId() != null) {
                // Chỉ dùng assignedPtId từ request nếu gói chưa có PT
                assignedPt = userRepository.findById(request.getAssignedPtId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                if (assignedPt.getRole() != Role.PT) {
                    throw new IllegalArgumentException("Người dùng (ID: " + assignedPt.getId() + ") không phải là PT.");
                }
            } else {
                throw new IllegalArgumentException("Gói PT phải có PT được gán. Vui lòng gán PT khi tạo gói hoặc khi đăng ký.");
            }
            
            newSubscriptionBuilder.assignedPt(assignedPt);
            
            // Validate timeSlot là bắt buộc cho PT_SESSION
            if (request.getTimeSlot() != null) {
                // Kiểm tra xem khung giờ đã được đặt chưa
                boolean timeSlotTaken = memberPackageRepository.existsByAssignedPt_IdAndTimeSlotAndStatus(
                        assignedPt.getId(), request.getTimeSlot(), SubscriptionStatus.ACTIVE);
                
                if (timeSlotTaken) {
                    throw new IllegalStateException("Khung giờ " + request.getTimeSlot().getDisplayName() + 
                            " đã được đặt bởi hội viên khác. Vui lòng chọn khung giờ khác.");
                }
                
                newSubscriptionBuilder.timeSlot(request.getTimeSlot());
            }
        }

        MemberPackage savedSubscription = memberPackageRepository.save(newSubscriptionBuilder.build());

        // Áp dụng FULL giảm giá (tự động + promotion) cho gia hạn
        java.math.BigDecimal amount2 = calculateFinalPrice(member, newGymPackage);
        User currentUser2 = authenticationService.getCurrentAuthenticatedUser();
        Transaction tx2 = Transaction.builder()
                .amount(amount2)
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
                .kind(TransactionKind.SUBSCRIPTION_RENEW)
                .transactionDate(java.time.OffsetDateTime.now())
                .createdBy(currentUser2)
                .memberPackage(savedSubscription)
                .sale(null)
                .build();
        transactionRepository.save(tx2);

        return SubscriptionResponseDTO.fromMemberPackage(savedSubscription);
    }

    // Hủy gói tập
    @Transactional
    public void cancelSubscription(Long subscriptionId) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể hủy các gói tập đang hoạt động.");
        }

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        memberPackageRepository.save(subscription);
    }

    // Hủy gói tập kèm lý do
    @Transactional
    public void cancelSubscription(Long subscriptionId, String reason) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể hủy các gói tập đang hoạt động.");
        }
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            subscription.setCancellationReason(reason);
        }
        memberPackageRepository.save(subscription);
    }

    @Transactional
    public java.math.BigDecimal refundSubscription(Long subscriptionId, PaymentMethod paymentMethod) {
        System.out.println("=== REFUND SUBSCRIPTION ===");
        System.out.println("Subscription ID: " + subscriptionId);
        
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        System.out.println("Current status: " + subscription.getStatus());
        
        if (subscription.getStatus() != SubscriptionStatus.CANCELLED) {
            throw new IllegalStateException("Vui lòng hủy gói trước khi hoàn tiền.");
        }

        GymPackage pkg = subscription.getGymPackage();
        System.out.println("Package type: " + pkg.getPackageType());
        System.out.println("Package price: " + pkg.getPrice());
        
        // Tính tổng số tiền đã thanh toán (tổng các transaction không phải REFUND)
        java.math.BigDecimal paidAmount = transactionRepository.findAllByMemberPackage_Id(subscription.getId())
                .stream()
                .filter(tx -> tx.getKind() != com.gym.service.gymmanagementservice.models.TransactionKind.REFUND)
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        // Nếu không có transaction nào, dùng giá gói làm mặc định
        if (paidAmount.compareTo(java.math.BigDecimal.ZERO) == 0) {
            paidAmount = pkg.getPrice();
        }
        
        // Trừ đi các transaction REFUND (nếu có)
        java.math.BigDecimal refundedAmount = transactionRepository.findAllByMemberPackage_Id(subscription.getId())
                .stream()
                .filter(tx -> tx.getKind() == com.gym.service.gymmanagementservice.models.TransactionKind.REFUND)
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        
        paidAmount = paidAmount.subtract(refundedAmount);
        
        System.out.println("Paid amount: " + paidAmount);

        java.math.BigDecimal refund = java.math.BigDecimal.ZERO;
        java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

        if (pkg.getPackageType() == PackageType.GYM_ACCESS) {
            // Tính tổng số ngày: ưu tiên durationMonths, fallback về durationDays
            long totalDays;
            if (pkg.getDurationMonths() != null && pkg.getDurationMonths() > 0) {
                // Tính số ngày từ durationMonths
                java.time.OffsetDateTime startDate = subscription.getStartDate();
                java.time.OffsetDateTime calculatedEndDate = startDate.plusMonths(pkg.getDurationMonths());
                totalDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, calculatedEndDate);
            } else if (pkg.getDurationDays() != null && pkg.getDurationDays() > 0) {
                totalDays = pkg.getDurationDays();
            } else {
                throw new IllegalStateException("Gói GYM_ACCESS phải có durationMonths hoặc durationDays.");
            }
            
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now, subscription.getEndDate());
            System.out.println("Total days: " + totalDays);
            System.out.println("Remaining days: " + remainingDays);
            
            if (remainingDays < 0) remainingDays = 0;
            if (totalDays <= 0) {
                throw new IllegalStateException("Tổng số ngày của gói phải lớn hơn 0.");
            }
            
            java.math.BigDecimal daily = paidAmount.divide(java.math.BigDecimal.valueOf(totalDays), 2, java.math.RoundingMode.HALF_UP);
            refund = daily.multiply(java.math.BigDecimal.valueOf(remainingDays));
            
            // Làm tròn đến hàng trăm
            refund = refund.divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                          .multiply(java.math.BigDecimal.valueOf(100));
            
            System.out.println("Daily rate: " + daily);
            System.out.println("Refund amount (rounded): " + refund);
        } else {
            int totalSessions = pkg.getNumberOfSessions() != null ? pkg.getNumberOfSessions() : 0;
            int remaining = subscription.getRemainingSessions() != null ? subscription.getRemainingSessions() : 0;
            
            System.out.println("Total sessions: " + totalSessions);
            System.out.println("Remaining sessions: " + remaining);
            
            if (totalSessions > 0 && remaining > 0) {
                java.math.BigDecimal per = paidAmount.divide(java.math.BigDecimal.valueOf(totalSessions), 2, java.math.RoundingMode.HALF_UP);
                refund = per.multiply(java.math.BigDecimal.valueOf(remaining));
                
                // Làm tròn đến hàng trăm
                refund = refund.divide(java.math.BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP)
                              .multiply(java.math.BigDecimal.valueOf(100));
                
                System.out.println("Per session rate: " + per);
                System.out.println("Refund amount (rounded): " + refund);
            }
        }

        System.out.println("Final refund amount: " + refund);

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Transaction tx = Transaction.builder()
                .amount(refund.negate())
                .paymentMethod(paymentMethod)
                .status(TransactionStatus.COMPLETED)
                .kind(TransactionKind.REFUND)
                .transactionDate(now)
                .createdBy(currentUser)
                .memberPackage(subscription)
                .sale(null)
                .build();
        transactionRepository.save(tx);
        
        System.out.println("Refund transaction saved successfully");

        return refund;
    }

    // Đóng băng gói tập
    @Transactional
    public SubscriptionResponseDTO freezeSubscription(Long subscriptionId, FreezeRequestDTO request) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ có thể đóng băng các gói tập đang hoạt động.");
        }

        // Cộng thêm số ngày đóng băng vào ngày kết thúc
        subscription.setEndDate(subscription.getEndDate().plusDays(request.getFreezeDays()));
        subscription.setStatus(SubscriptionStatus.FROZEN);

        MemberPackage updatedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(updatedSubscription);
    }

    // Mở lại gói tập đã đóng băng
    @Transactional
    public SubscriptionResponseDTO unfreezeSubscription(Long subscriptionId) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));

        if(subscription.getStatus() != SubscriptionStatus.FROZEN) {
            throw new IllegalStateException("Gói tập này không ở trạng thái đóng băng.");
        }

        subscription.setStatus(SubscriptionStatus.ACTIVE);
        MemberPackage updatedSubscription = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(updatedSubscription);
    }

    /**
     * Nâng cấp gói tập:
     * - Chỉ áp dụng cho gói GYM_ACCESS và PER_VISIT (có thời hạn)
     * - Gói mới phải GIÁ CAO HƠN gói cũ
     * - Tính hoàn tiền theo tỷ lệ thời gian còn lại
     * - Giữ nguyên thời gian kết thúc của gói cũ
     * - Chỉ thu tiền chênh lệch
     */
    @Transactional
    public SubscriptionResponseDTO upgradeSubscription(Long subscriptionId, Long newPackageId, PaymentMethod paymentMethod) {
        MemberPackage current = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ nâng cấp từ gói đang hoạt động.");
        }

        GymPackage currentPkg = current.getGymPackage();
        GymPackage newPkg = gymPackageRepository.findById(newPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + newPackageId));

        // Kiểm tra gói mới phải đắt hơn
        if (newPkg.getPrice().compareTo(currentPkg.getPrice()) <= 0) {
            throw new IllegalStateException("Gói mới phải có giá cao hơn gói hiện tại để nâng cấp.");
        }

        // Tính giá trị hoàn lại từ gói cũ
        OffsetDateTime now = OffsetDateTime.now();
        java.math.BigDecimal refundValue = java.math.BigDecimal.ZERO;

        if (currentPkg.getPackageType() == PackageType.PT_SESSION) {
            // Gói PT: Tính hoàn lại theo số buổi còn lại
            Integer totalSessions = currentPkg.getNumberOfSessions() != null ? currentPkg.getNumberOfSessions() : 0;
            Integer remainingSessions = current.getRemainingSessions() != null ? current.getRemainingSessions() : 0;
            
            if (totalSessions <= 0) {
                throw new IllegalStateException("Gói PT không hợp lệ (không có số buổi).");
            }
            
            if (remainingSessions <= 0) {
                throw new IllegalStateException("Gói PT đã hết buổi, không thể nâng cấp.");
            }

            // Giá trị 1 buổi của gói PT (tính từ giá gói ban đầu)
            java.math.BigDecimal perSessionValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalSessions), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
            
            // Chỉ tính hoàn lại cho số buổi ban đầu của gói (không tính buổi gia hạn)
            // Nếu remainingSessions > totalSessions (đã gia hạn), chỉ tính cho totalSessions buổi đầu
            int sessionsToRefund = Math.min(remainingSessions, totalSessions);
            
            // Giá trị hoàn lại = giá trị 1 buổi × số buổi cần hoàn lại (tối đa = tổng buổi ban đầu)
            refundValue = perSessionValue.multiply(java.math.BigDecimal.valueOf(sessionsToRefund));
            
            // Đảm bảo giá trị hoàn lại không vượt quá giá đã thanh toán ban đầu
            if (refundValue.compareTo(currentPkg.getPrice()) > 0) {
                refundValue = currentPkg.getPrice();
            }
            
        } else if (currentPkg.getPackageType() == PackageType.GYM_ACCESS || currentPkg.getPackageType() == PackageType.PER_VISIT) {
            // Gói thời gian: Tính hoàn lại theo ngày
            if (current.getEndDate() == null) {
                throw new IllegalStateException("Gói tập không có ngày kết thúc, không thể nâng cấp.");
            }
            
            long totalDays = currentPkg.getDurationDays();
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now, current.getEndDate());
            
            if (remainingDays <= 0) {
                throw new IllegalStateException("Gói tập đã hết hạn, không thể nâng cấp.");
            }

            if (totalDays <= 0) {
                throw new IllegalStateException("Gói tập không hợp lệ (không có thời hạn).");
            }

            // Giá trị 1 ngày của gói cũ
            java.math.BigDecimal dailyValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalDays), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
            
            // Giá trị hoàn lại = giá trị 1 ngày × số ngày còn lại
            refundValue = dailyValue.multiply(java.math.BigDecimal.valueOf(remainingDays));
        } else {
            throw new IllegalStateException("Loại gói tập không hỗ trợ nâng cấp.");
        }

        // Hủy gói cũ
        current.setStatus(SubscriptionStatus.CANCELLED);
        memberPackageRepository.save(current);

        // Tạo gói mới
        Member member = current.getMember();
        OffsetDateTime newStartDate = now;
        OffsetDateTime newEndDate;
        
        if (newPkg.getPackageType() == PackageType.GYM_ACCESS) {
            // Gói GYM_ACCESS: Ưu tiên durationMonths, fallback về durationDays
            if (newPkg.getDurationMonths() != null && newPkg.getDurationMonths() > 0) {
                newEndDate = newStartDate.plusMonths(newPkg.getDurationMonths());
            } else if (newPkg.getDurationDays() != null && newPkg.getDurationDays() > 0) {
                newEndDate = newStartDate.plusDays(newPkg.getDurationDays());
            } else {
                throw new IllegalStateException("Gói GYM_ACCESS phải có durationMonths hoặc durationDays.");
            }
        } else if (newPkg.getPackageType() == PackageType.PER_VISIT) {
            // Gói PER_VISIT: Tính endDate từ durationDays
            newEndDate = newStartDate.plusDays(newPkg.getDurationDays());
        } else {
            // Gói PT: Không có endDate hoặc dùng endDate của gói cũ nếu có
            newEndDate = current.getEndDate(); // Có thể null cho gói PT
        }
        
        MemberPackage.MemberPackageBuilder builder = MemberPackage.builder()
                .member(member)
                .gymPackage(newPkg)
                .status(SubscriptionStatus.ACTIVE)
                .startDate(newStartDate)
                .endDate(newEndDate);

        if (newPkg.getPackageType() == PackageType.PER_VISIT) {
            // Gói theo lượt: Lấy số buổi từ gói mới (không cộng dồn)
            builder.remainingSessions(newPkg.getNumberOfSessions());
        } else if (newPkg.getPackageType() == PackageType.PT_SESSION) {
            // Gói PT mới: Lấy số buổi từ gói
            builder.remainingSessions(newPkg.getNumberOfSessions());
        }

        MemberPackage upgraded = memberPackageRepository.save(builder.build());

        // Tính tiền phải trả = Giá gói mới (có giảm giá) - Giá trị hoàn lại
        java.math.BigDecimal newPackagePrice = calculateFinalPrice(member, newPkg);
        java.math.BigDecimal amountToPay = newPackagePrice.subtract(refundValue);
        
        // Đảm bảo không âm
        if (amountToPay.compareTo(java.math.BigDecimal.ZERO) < 0) {
            amountToPay = java.math.BigDecimal.ZERO;
        }

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Transaction tx = Transaction.builder()
                .amount(amountToPay)
                .paymentMethod(paymentMethod)
                .status(TransactionStatus.COMPLETED)
                .kind(TransactionKind.SUBSCRIPTION_UPGRADE)
                .transactionDate(now)
                .createdBy(currentUser)
                .memberPackage(upgraded)
                .sale(null)
                .build();
        transactionRepository.save(tx);

        return SubscriptionResponseDTO.fromMemberPackage(upgraded);
    }

    @Transactional
    public SubscriptionResponseDTO transferSubscription(Long subscriptionId, Long toMemberId) {
        MemberPackage subscription = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        if (subscription.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ chuyển nhượng gói đang hoạt động.");
        }
        Member target = memberRepository.findById(toMemberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + toMemberId));

        subscription.setMember(target);
        MemberPackage saved = memberPackageRepository.save(subscription);
        return SubscriptionResponseDTO.fromMemberPackage(saved);
    }

    /**
     * Lấy danh sách các time slot khả dụng cho một gói PT.
     * Nếu gói đã có PT được gán, trả về các time slot còn trống của PT đó.
     * 
     * @param packageId ID của gói tập
     * @return Danh sách các time slot khả dụng với thông tin available/taken
     */
    public java.util.List<java.util.Map<String, Object>> getAvailableTimeSlots(Long packageId) {
        GymPackage gymPackage = gymPackageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + packageId));

        if (gymPackage.getPackageType() != PackageType.PT_SESSION) {
            throw new IllegalArgumentException("Chỉ gói PT mới có time slot.");
        }

        if (gymPackage.getAssignedPt() == null) {
            throw new IllegalArgumentException("Gói này chưa có PT được gán. Vui lòng gán PT trước khi xem time slot.");
        }

        Long ptId = gymPackage.getAssignedPt().getId();
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();

        // Lấy tất cả các time slot có sẵn
        for (TimeSlot slot : TimeSlot.values()) {
            boolean isTaken = memberPackageRepository.existsByAssignedPt_IdAndTimeSlotAndStatus(
                    ptId, slot, SubscriptionStatus.ACTIVE);

            java.util.Map<String, Object> slotInfo = new java.util.HashMap<>();
            slotInfo.put("timeSlot", slot.name());
            slotInfo.put("displayName", slot.getDisplayName());
            slotInfo.put("startTime", slot.getStartTime().toString());
            slotInfo.put("endTime", slot.getEndTime().toString());
            slotInfo.put("available", !isTaken);
            result.add(slotInfo);
        }

        return result;
    }

    /**
     * Tính toán giá trị hoàn lại và số tiền phải bù khi nâng cấp (không thực hiện nâng cấp)
     */
    @Transactional(readOnly = true)
    public com.gym.service.gymmanagementservice.dtos.UpgradeCalculationDTO calculateUpgradePrice(Long subscriptionId, Long newPackageId) {
        MemberPackage current = memberPackageRepository.findById(subscriptionId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói đăng ký với ID: " + subscriptionId));
        
        if (current.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Chỉ nâng cấp từ gói đang hoạt động.");
        }

        GymPackage currentPkg = current.getGymPackage();
        GymPackage newPkg = gymPackageRepository.findById(newPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + newPackageId));

        // Kiểm tra gói mới phải đắt hơn
        if (newPkg.getPrice().compareTo(currentPkg.getPrice()) <= 0) {
            throw new IllegalStateException("Gói mới phải có giá cao hơn gói hiện tại để nâng cấp.");
        }

        // Tính giá trị hoàn lại từ gói cũ (giống logic trong upgradeSubscription)
        OffsetDateTime now = OffsetDateTime.now();
        java.math.BigDecimal refundValue = java.math.BigDecimal.ZERO;

        if (currentPkg.getPackageType() == PackageType.PT_SESSION) {
            // Gói PT: Tính hoàn lại theo số buổi còn lại
            Integer totalSessions = currentPkg.getNumberOfSessions() != null ? currentPkg.getNumberOfSessions() : 0;
            Integer remainingSessions = current.getRemainingSessions() != null ? current.getRemainingSessions() : 0;
            
            if (totalSessions <= 0) {
                throw new IllegalStateException("Gói PT không hợp lệ (không có số buổi).");
            }
            
            if (remainingSessions <= 0) {
                throw new IllegalStateException("Gói PT đã hết buổi, không thể nâng cấp.");
            }

            // Giá trị 1 buổi của gói PT (tính từ giá gói ban đầu)
            java.math.BigDecimal perSessionValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalSessions), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
            
            // Chỉ tính hoàn lại cho số buổi ban đầu của gói (không tính buổi gia hạn)
            int sessionsToRefund = Math.min(remainingSessions, totalSessions);
            
            // Giá trị hoàn lại = giá trị 1 buổi × số buổi cần hoàn lại
            refundValue = perSessionValue.multiply(java.math.BigDecimal.valueOf(sessionsToRefund));
            
            // Đảm bảo giá trị hoàn lại không vượt quá giá đã thanh toán ban đầu
            if (refundValue.compareTo(currentPkg.getPrice()) > 0) {
                refundValue = currentPkg.getPrice();
            }
            
        } else if (currentPkg.getPackageType() == PackageType.GYM_ACCESS || currentPkg.getPackageType() == PackageType.PER_VISIT) {
            // Gói thời gian: Tính hoàn lại theo ngày
            if (current.getEndDate() == null) {
                throw new IllegalStateException("Gói tập không có ngày kết thúc, không thể nâng cấp.");
            }
            
            long totalDays = currentPkg.getDurationDays();
            long remainingDays = java.time.temporal.ChronoUnit.DAYS.between(now, current.getEndDate());
            
            if (remainingDays <= 0) {
                throw new IllegalStateException("Gói tập đã hết hạn, không thể nâng cấp.");
            }

            if (totalDays <= 0) {
                throw new IllegalStateException("Gói tập không hợp lệ (không có thời hạn).");
            }

            // Giá trị 1 ngày của gói cũ
            java.math.BigDecimal dailyValue = currentPkg.getPrice().divide(
                java.math.BigDecimal.valueOf(totalDays), 
                2, 
                java.math.RoundingMode.HALF_UP
            );
            
            // Giá trị hoàn lại = giá trị 1 ngày × số ngày còn lại
            refundValue = dailyValue.multiply(java.math.BigDecimal.valueOf(remainingDays));
        } else {
            throw new IllegalStateException("Loại gói tập không hỗ trợ nâng cấp.");
        }

        // Tính giá gói mới (có thể có giảm giá)
        Member member = current.getMember();
        java.math.BigDecimal newPackagePrice = calculateFinalPrice(member, newPkg);
        
        // Số tiền phải bù thêm
        java.math.BigDecimal amountToPay = newPackagePrice.subtract(refundValue);
        if (amountToPay.compareTo(java.math.BigDecimal.ZERO) < 0) {
            amountToPay = java.math.BigDecimal.ZERO;
        }

        return com.gym.service.gymmanagementservice.dtos.UpgradeCalculationDTO.builder()
                .currentPackagePrice(currentPkg.getPrice())
                .newPackagePrice(newPackagePrice)
                .refundValue(refundValue)
                .amountToPay(amountToPay)
                .build();
    }
}
