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

    @Transactional
    public SubscriptionResponseDTO createSubscription(SubscriptionRequestDTO request) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + request.getMemberId()));

        GymPackage gymPackage = gymPackageRepository.findById(request.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + request.getPackageId()));

        User currentUser = authenticationService.getCurrentAuthenticatedUser(); // Lấy user đang thực hiện

        MemberPackage.MemberPackageBuilder subscriptionBuilder = MemberPackage.builder()
                .member(member)
                .gymPackage(gymPackage)
                .status(SubscriptionStatus.ACTIVE); // Mặc định là ACTIVE khi tạo mới

        // === LOGIC PHÂN LOẠI ===

        if (gymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            // 1. XỬ LÝ GÓI GYM ACCESS

            boolean hasActiveGymPackage = memberPackageRepository
                    .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                            member.getId(), SubscriptionStatus.ACTIVE, PackageType.GYM_ACCESS)
                    .isPresent();

            if (hasActiveGymPackage) {
                throw new IllegalStateException("Hội viên này đã có một gói tập GYM ACCESS đang hoạt động. Vui lòng sử dụng chức năng Gia hạn.");
            }

            OffsetDateTime startDate = OffsetDateTime.now();
            OffsetDateTime endDate = startDate.plusDays(gymPackage.getDurationDays());
            subscriptionBuilder.startDate(startDate).endDate(endDate);

        } else if (gymPackage.getPackageType() == PackageType.PT_SESSION) {
            // 2. XỬ LÝ GÓI PT
            subscriptionBuilder.remainingSessions(gymPackage.getNumberOfSessions());
            if (request.getAssignedPtId() != null) {
                User pt = userRepository.findById(request.getAssignedPtId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                if (pt.getRole() != Role.PT) {
                    throw new IllegalArgumentException("Người dùng (ID: " + pt.getId() + ") không phải là PT.");
                }
                subscriptionBuilder.assignedPt(pt);
            }

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
        MemberPackage savedSubscription = memberPackageRepository.save(subscriptionBuilder.build());

        // === TẠO GIAO DỊCH (TRANSACTION) ===
        Transaction transaction = Transaction.builder()
                .amount(gymPackage.getPrice())
                .paymentMethod(request.getPaymentMethod())
                .status(TransactionStatus.COMPLETED)
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

                // Cập nhật PT được gán nếu có yêu cầu
                if (request.getAssignedPtId() != null) {
                    User pt = userRepository.findById(request.getAssignedPtId())
                            .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                    if (pt.getRole() != Role.PT) {
                        throw new IllegalArgumentException("Người dùng (ID: " + pt.getId() + ") không phải là PT.");
                    }
                    packageToRenew.setAssignedPt(pt); // Cập nhật PT cho gói đang gia hạn
                }

                // Nếu gói đã hết hạn (remainingSessions = 0) thì kích hoạt lại
                if (packageToRenew.getStatus() == SubscriptionStatus.EXPIRED) {
                    packageToRenew.setStatus(SubscriptionStatus.ACTIVE);
                }

                MemberPackage savedSubscription = memberPackageRepository.save(packageToRenew);
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
                    .orElseThrow(() -> new IllegalStateException("Hội viên không có gói tập GYM ACCESS nào đang hoạt động để gia hạn."));

            // Ngày bắt đầu của gói mới là ngày kết thúc của gói cũ
            OffsetDateTime newStartDate = lastActivePackage.getEndDate();
            OffsetDateTime newEndDate = newStartDate.plusDays(newGymPackage.getDurationDays());

            newSubscriptionBuilder.startDate(newStartDate).endDate(newEndDate);

        } else if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {
            // Trường hợp này là mua mới gói PT (vì không tìm thấy gói cùng loại ở trên)
            // Logic giống createSubscription
            newSubscriptionBuilder.remainingSessions(newGymPackage.getNumberOfSessions());

            if (request.getAssignedPtId() != null) {
                User pt = userRepository.findById(request.getAssignedPtId())
                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
                if (pt.getRole() != Role.PT) {
                    throw new IllegalArgumentException("Người dùng (ID: " + pt.getId() + ") không phải là PT.");
                }
                newSubscriptionBuilder.assignedPt(pt);
            }
        }

        MemberPackage savedSubscription = memberPackageRepository.save(newSubscriptionBuilder.build());
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
}