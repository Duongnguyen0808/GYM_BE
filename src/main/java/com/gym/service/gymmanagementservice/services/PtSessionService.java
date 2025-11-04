package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.PtSessionLogRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PtSessionService {

    private final MemberPackageRepository memberPackageRepository;
    private final PtSessionLogRepository ptSessionLogRepository;
    private final AuthenticationService authenticationService;

    @Transactional
    public PtSessionLog logPtSession(Long memberPackageId, String notes) {
        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        Role currentUserRole = currentUser.getRole();

        MemberPackage ptPackage = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói PT với ID: " + memberPackageId));

        // === VALIDATIONS ===
        if (ptPackage.getGymPackage().getPackageType() != PackageType.PT_SESSION) {
            throw new IllegalArgumentException("Gói này không phải là gói PT.");
        }
        if (ptPackage.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói PT này không ở trạng thái hoạt động.");
        }
        if (ptPackage.getRemainingSessions() == null || ptPackage.getRemainingSessions() <= 0) {
            throw new IllegalStateException("Gói PT đã hết số buổi tập.");
        }

        // Kiểm tra quyền của PT
        if (currentUserRole == Role.PT) {
            if (ptPackage.getAssignedPt() == null || !ptPackage.getAssignedPt().getId().equals(currentUser.getId())) {
                throw new AccessDeniedException("PT không được gán cho gói tập này.");
            }
        }

        // 1. Trừ 1 buổi tập
        ptPackage.setRemainingSessions(ptPackage.getRemainingSessions() - 1);

        // 2. Nếu hết buổi, cập nhật status
        if (ptPackage.getRemainingSessions() == 0) {
            ptPackage.setStatus(SubscriptionStatus.EXPIRED);
        }
        memberPackageRepository.save(ptPackage);

        // 3. Tạo log
        PtSessionLog log = PtSessionLog.builder()
                .memberPackage(ptPackage)
                .ptUser(currentUser) // Log PT đang đăng nhập là người dạy
                .sessionDate(OffsetDateTime.now())
                .notes(notes)
                .build();

        return ptSessionLogRepository.save(log);
    }
}