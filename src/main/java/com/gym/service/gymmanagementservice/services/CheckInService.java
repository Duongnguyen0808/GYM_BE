package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime; // <-- IMPORT MỚI
import java.time.OffsetDateTime;
import java.time.ZoneId; // <-- IMPORT MỚI
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckInService {

    private final MemberRepository memberRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final CheckInLogRepository checkInLogRepository;

    // MỚI: Định nghĩa múi giờ của phòng gym (để kiểm tra off-peak)
    private final ZoneId gymTimeZone = ZoneId.of("Asia/Ho_Chi_Minh");

    /**
     * HÀM MỚI: Kiểm tra xem thời gian check-in có nằm trong khung giờ cho phép không
     * @param now Thời điểm check-in (UTC)
     * @param pkg Gói tập (chứa
     * @return true nếu hợp lệ, false nếu vi phạm
     */
    private boolean isCheckInTimeValid(OffsetDateTime now, MemberPackage pkg) {
        LocalTime startTime = pkg.getGymPackage().getStartTimeLimit();
        LocalTime endTime = pkg.getGymPackage().getEndTimeLimit();

        // Nếu gói không có giới hạn (startTime hoặc endTime là null), luôn hợp lệ
        if (startTime == null || endTime == null) {
            return true;
        }

        // Lấy giờ địa phương tại phòng gym (VD: 10:30 sáng)
        LocalTime localCheckInTime = now.atZoneSameInstant(gymTimeZone).toLocalTime();

        // Kiểm tra
        // Ví dụ: [09:00 - 16:00]
        // Hợp lệ: localCheckInTime >= 09:00 VÀ localCheckInTime <= 16:00
        // (Lưu ý: isBefore/isAfter không bao gồm bằng)
        boolean isAfterOrEqualStart = !localCheckInTime.isBefore(startTime);
        boolean isBeforeOrEqualEnd = !localCheckInTime.isAfter(endTime);

        return isAfterOrEqualStart && isBeforeOrEqualEnd;
    }


    @Transactional
    public CheckInResponseDTO performCheckIn(CheckInRequestDTO request) {
        OffsetDateTime now = OffsetDateTime.now(); // Dùng 1 mốc thời gian (UTC)
        Optional<Member> memberOpt = memberRepository.findByBarcode(request.getBarcode());

        // Không tìm thấy hội viên
        if (memberOpt.isEmpty()) {
            createLog(null, CheckInStatus.FAILED_MEMBER_NOT_FOUND, "Mã vạch không tồn tại trong hệ thống.");
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_MEMBER_NOT_FOUND)
                    .message("Không tìm thấy hội viên!")
                    .build();
        }

        Member member = memberOpt.get();

        // ƯU TIÊN 1: Kiểm tra gói GYM_ACCESS (vào cửa không giới hạn)
        Optional<MemberPackage> activeGymAccessPackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeOrderByEndDateDesc(
                        member.getId(),
                        SubscriptionStatus.ACTIVE,
                        PackageType.GYM_ACCESS
                );

        if (activeGymAccessPackageOpt.isPresent()) {
            MemberPackage activePackage = activeGymAccessPackageOpt.get();

            if (activePackage.getEndDate().isBefore(now)) {
                log.warn("Gói GYM_ACCESS ID {} có status ACTIVE nhưng đã hết hạn.", activePackage.getId());
            } else {
                // KIỂM TRA KHUNG GIỜ
                if (!isCheckInTimeValid(now, activePackage)) {
                    String errorMsg = String.format("Gói tập [%s] chỉ hợp lệ trong khung giờ %s - %s.",
                            activePackage.getGymPackage().getName(),
                            activePackage.getGymPackage().getStartTimeLimit(),
                            activePackage.getGymPackage().getEndTimeLimit());

                    log.warn("Check-in thất bại (Off-Peak) cho hội viên {}: {}", member.getId(), errorMsg);
                    createLog(member, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

                    // Trả về lỗi Off-Peak và dừng lại (không tìm gói PER_VISIT nữa)
                    return CheckInResponseDTO.builder()
                            .status(CheckInStatus.FAILED_OFF_PEAK_TIME)
                            .message(errorMsg)
                            .memberFullName(member.getFullName())
                            .packageName(activePackage.getGymPackage().getName())
                            .packageEndDate(activePackage.getEndDate())
                            .build();
                }

                // Check-in thành công với gói GYM_ACCESS
                createLog(member, CheckInStatus.SUCCESS, "Check-in thành công (Gói Gym Access).");
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.SUCCESS)
                        .message("Check-in thành công!")
                        .memberFullName(member.getFullName())
                        .packageName(activePackage.getGymPackage().getName())
                        .packageEndDate(activePackage.getEndDate())
                        .build();
            }
        }

        // ƯU TIÊN 2: Kiểm tra gói PER_VISIT (vào cửa theo lượt)
        Optional<MemberPackage> activePerVisitPackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeAndEndDateAfterAndRemainingSessionsGreaterThanOrderByEndDateAsc(
                        member.getId(),
                        SubscriptionStatus.ACTIVE,
                        PackageType.PER_VISIT,
                        now, // Phải còn hạn
                        0  // Phải còn lượt
                );

        if (activePerVisitPackageOpt.isPresent()) {
            MemberPackage perVisitPackage = activePerVisitPackageOpt.get();

            // KIỂM TRA KHUNG GIỜ
            if (!isCheckInTimeValid(now, perVisitPackage)) {
                String errorMsg = String.format("Gói tập [%s] chỉ hợp lệ trong khung giờ %s - %s.",
                        perVisitPackage.getGymPackage().getName(),
                        perVisitPackage.getGymPackage().getStartTimeLimit(),
                        perVisitPackage.getGymPackage().getEndTimeLimit());

                log.warn("Check-in thất bại (Off-Peak) cho hội viên {}: {}", member.getId(), errorMsg);
                createLog(member, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

                // Trả về lỗi Off-Peak
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.FAILED_OFF_PEAK_TIME)
                        .message(errorMsg)
                        .memberFullName(member.getFullName())
                        .packageName(perVisitPackage.getGymPackage().getName())
                        .packageEndDate(perVisitPackage.getEndDate())
                        .build();
            }

            // TRỪ 1 LƯỢT CHECK-IN
            int remaining = perVisitPackage.getRemainingSessions() - 1;
            perVisitPackage.setRemainingSessions(remaining);

            String message = String.format("Check-in thành công (Gói Per-Visit). Còn lại %d lượt.", remaining);
            log.info("Hội viên {}: {}", member.getId(), message);

            if (remaining == 0) {
                perVisitPackage.setStatus(SubscriptionStatus.EXPIRED);
                log.info("Gói Per-Visit ID {} đã hết lượt và chuyển sang EXPIRED.", perVisitPackage.getId());
            }

            memberPackageRepository.save(perVisitPackage); // Lưu lại số lượt mới

            createLog(member, CheckInStatus.SUCCESS, message);
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message(message)
                    .memberFullName(member.getFullName())
                    .packageName(perVisitPackage.getGymPackage().getName())
                    .packageEndDate(perVisitPackage.getEndDate())
                    .build();
        }

        // KHÔNG TÌM THẤY GÓI NÀO HỢP LỆ
        createLog(member, CheckInStatus.FAILED_NO_ACTIVE_PACKAGE, "Hội viên không có gói tập (Gym Access/Per-Visit) nào đang hoạt động.");
        return CheckInResponseDTO.builder()
                .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                .message("Hội viên không có gói tập nào đang hoạt động!")
                .memberFullName(member.getFullName())
                .build();
    }

    private void createLog(Member member, CheckInStatus status, String message) {
        CheckInLog log = CheckInLog.builder()
                .member(member)
                .checkInTime(OffsetDateTime.now())
                .status(status)
                .message(message)
                .build();
        checkInLogRepository.save(log);
    }
}