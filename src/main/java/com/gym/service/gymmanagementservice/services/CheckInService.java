package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.CheckInRequestDTO;
import com.gym.service.gymmanagementservice.dtos.CheckInResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.PtSessionLogRepository;
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
    private final DailyQrService dailyQrService;
    private final PtSessionLogRepository ptSessionLogRepository;

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
            // vẫn kiểm tra thứ trong tuần nếu có cấu hình allowedWeekdays
            return isWeekdayAllowed(now, pkg);
        }

        // Lấy giờ địa phương tại phòng gym (VD: 10:30 sáng)
        LocalTime localCheckInTime = now.atZoneSameInstant(gymTimeZone).toLocalTime();

        // Kiểm tra
        // Ví dụ: [09:00 - 16:00]
        // Hợp lệ: localCheckInTime >= 09:00 VÀ localCheckInTime <= 16:00
        // (Lưu ý: isBefore/isAfter không bao gồm bằng)
        boolean isAfterOrEqualStart = !localCheckInTime.isBefore(startTime);
        boolean isBeforeOrEqualEnd = !localCheckInTime.isAfter(endTime);

        return isAfterOrEqualStart && isBeforeOrEqualEnd && isWeekdayAllowed(now, pkg);
    }

    private boolean isWeekdayAllowed(OffsetDateTime now, MemberPackage pkg) {
        String allowed = pkg.getAllowedWeekdays();
        if (allowed == null || allowed.isBlank()) return true;
        java.util.Set<String> set = java.util.Arrays.stream(allowed.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toSet());
        String dow = now.atZoneSameInstant(gymTimeZone).getDayOfWeek().name().substring(0,3); // MON,TUE,...
        return set.contains(dow);
    }


    @Transactional
    public CheckInResponseDTO performCheckIn(CheckInRequestDTO request) {
        OffsetDateTime now = OffsetDateTime.now(); // Dùng 1 mốc thời gian (UTC)

        Optional<Member> memberOpt;
        Long memberIdFromToken = dailyQrService.verifyAndExtractMemberIdForToday(request.getBarcode());
        if (memberIdFromToken != null) {
            memberOpt = memberRepository.findById(memberIdFromToken);
        } else {
            memberOpt = memberRepository.findByBarcode(request.getBarcode());
        }

        // Không tìm thấy hội viên
        if (memberOpt.isEmpty()) {
            createLog(null, null, CheckInStatus.FAILED_MEMBER_NOT_FOUND, "Mã vạch/QR không tồn tại hoặc hết hạn.");
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_MEMBER_NOT_FOUND)
                    .message("Không tìm thấy hội viên!")
                    .build();
        }

        Member member = memberOpt.get();

        Optional<CheckInLog> openLogOpt = checkInLogRepository
                .findTopByMemberIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(member.getId());
        if (openLogOpt.isPresent()) {
            CheckInLog open = openLogOpt.get();
            MemberPackage pkg = open.getMemberPackage();
            String msg = "Check-in thành công!";
            if (pkg != null && pkg.getGymPackage() != null) {
                msg = "Đang tập";
            }
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message(msg)
                    .memberFullName(member.getFullName())
                    .packageName(pkg != null ? pkg.getGymPackage().getName() : null)
                    .packageEndDate(pkg != null ? pkg.getEndDate() : null)
                    .build();
        }

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
                    createLog(member, activePackage, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

                    // Trả về lỗi Off-Peak và dừng lại (không tìm gói PER_VISIT nữa)
                    return CheckInResponseDTO.builder()
                            .status(CheckInStatus.FAILED_OFF_PEAK_TIME)
                            .message(errorMsg)
                            .memberFullName(member.getFullName())
                            .packageName(activePackage.getGymPackage().getName())
                            .packageEndDate(activePackage.getEndDate())
                            .build();
                }

                createLog(member, activePackage, CheckInStatus.SUCCESS, "Đang tập (Gói thời hạn).");
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.SUCCESS)
                        .message("Đang tập")
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
                createLog(member, perVisitPackage, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);

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

            String message = String.format("Đang tập — Còn lại %d lượt.", remaining);
            log.info("Hội viên {}: {}", member.getId(), message);

            if (remaining == 0) {
                perVisitPackage.setStatus(SubscriptionStatus.EXPIRED);
                log.info("Gói Per-Visit ID {} đã hết lượt và chuyển sang EXPIRED.", perVisitPackage.getId());
            }

            memberPackageRepository.save(perVisitPackage);
            createLog(member, perVisitPackage, CheckInStatus.SUCCESS, message);
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message(message)
                    .memberFullName(member.getFullName())
                    .packageName(perVisitPackage.getGymPackage().getName())
                    .packageEndDate(perVisitPackage.getEndDate())
                    .build();
        }

        // ƯU TIÊN 3: Kiểm tra gói PT_SESSION (gói PT)
        Optional<MemberPackage> activePtPackageOpt = memberPackageRepository
                .findFirstByMemberIdAndStatusAndGymPackage_PackageTypeAndRemainingSessionsGreaterThanOrderByEndDateDesc(
                        member.getId(),
                        SubscriptionStatus.ACTIVE,
                        PackageType.PT_SESSION,
                        0  // Phải còn buổi
                );

        if (activePtPackageOpt.isPresent()) {
            MemberPackage ptPackage = activePtPackageOpt.get();

            // Kiểm tra gói PT còn hạn (nếu có endDate)
            if (ptPackage.getEndDate() != null && ptPackage.getEndDate().isBefore(now)) {
                log.warn("Gói PT_SESSION ID {} có status ACTIVE nhưng đã hết hạn.", ptPackage.getId());
            } else {
                // TRỪ 1 BUỔI TẬP PT
                int remaining = ptPackage.getRemainingSessions() != null ? ptPackage.getRemainingSessions() - 1 : -1;
                if (remaining < 0) {
                    createLog(member, ptPackage, CheckInStatus.FAILED_NO_ACTIVE_PACKAGE, "Gói PT đã hết buổi tập.");
                    return CheckInResponseDTO.builder()
                            .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                            .message("Gói PT đã hết buổi tập!")
                            .memberFullName(member.getFullName())
                            .packageName(ptPackage.getGymPackage().getName())
                            .packageEndDate(ptPackage.getEndDate())
                            .build();
                }

                ptPackage.setRemainingSessions(remaining);
                String message = String.format("Đang tập PT — Còn lại %d buổi.", remaining);
                log.info("Hội viên {}: {}", member.getId(), message);

                if (remaining == 0) {
                    ptPackage.setStatus(SubscriptionStatus.EXPIRED);
                    log.info("Gói PT_SESSION ID {} đã hết buổi và chuyển sang EXPIRED.", ptPackage.getId());
                }

                memberPackageRepository.save(ptPackage);
                
                // Tạo PtSessionLog để hiển thị trong lịch tập
                User ptUser = ptPackage.getAssignedPt();
                if (ptUser == null) {
                    // Nếu không có PT được gán, tìm PT đầu tiên hoặc để null
                    // Tạm thời để null, có thể cần xử lý sau
                    log.warn("Gói PT ID {} không có PT được gán, không tạo PtSessionLog.", ptPackage.getId());
                } else {
                    PtSessionLog ptSessionLog = PtSessionLog.builder()
                            .memberPackage(ptPackage)
                            .ptUser(ptUser)
                            .sessionDate(now)
                            .notes("Quét QR check-in")
                            .build();
                    ptSessionLogRepository.save(ptSessionLog);
                }
                
                createLog(member, ptPackage, CheckInStatus.SUCCESS, message);
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.SUCCESS)
                        .message(message)
                        .memberFullName(member.getFullName())
                        .packageName(ptPackage.getGymPackage().getName())
                        .packageEndDate(ptPackage.getEndDate())
                        .build();
            }
        }

        // KHÔNG TÌM THẤY GÓI NÀO HỢP LỆ
        createLog(member, null, CheckInStatus.FAILED_NO_ACTIVE_PACKAGE, "Hội viên không có gói tập (Gói thời hạn/Gói theo lượt/Gói PT) nào đang hoạt động.");
        return CheckInResponseDTO.builder()
                .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                .message("Hội viên không có gói tập nào đang hoạt động!")
                .memberFullName(member.getFullName())
                .build();
    }

    private void createLog(Member member, MemberPackage memberPackage, CheckInStatus status, String message) {
        CheckInLog log = CheckInLog.builder()
                .member(member)
                .memberPackage(memberPackage)
                .checkInTime(OffsetDateTime.now())
                .status(status)
                .message(message)
                .build();
        checkInLogRepository.save(log);
    }

    @Transactional
    public CheckInResponseDTO performCheckInBySubscription(Long memberPackageId) {
        OffsetDateTime now = OffsetDateTime.now();
        MemberPackage mp = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy đăng ký gói."));

        Member member = mp.getMember();

        Optional<CheckInLog> openLogOpt = checkInLogRepository
                .findTopByMemberPackageIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(memberPackageId);
        if (openLogOpt.isPresent()) {
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message("Đang tập")
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .packageEndDate(mp.getEndDate())
                    .build();
        }

        if (mp.getStatus() != SubscriptionStatus.ACTIVE) {
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                    .message("Gói không hoạt động")
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .build();
        }

        if (mp.getEndDate() != null && mp.getEndDate().isBefore(now)) {
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                    .message("Gói đã hết hạn")
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .build();
        }

        if (!isCheckInTimeValid(now, mp)) {
            String errorMsg = "Gói chỉ hợp lệ trong khung giờ quy định";
            createLog(member, mp, CheckInStatus.FAILED_OFF_PEAK_TIME, errorMsg);
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_OFF_PEAK_TIME)
                    .message(errorMsg)
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .packageEndDate(mp.getEndDate())
                    .build();
        }

        if (mp.getGymPackage().getPackageType() == PackageType.PER_VISIT) {
            int remaining = mp.getRemainingSessions() != null ? mp.getRemainingSessions() - 1 : -1;
            if (remaining < 0) {
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                        .message("Gói đã hết lượt")
                        .memberFullName(member.getFullName())
                        .packageName(mp.getGymPackage().getName())
                        .packageEndDate(mp.getEndDate())
                        .build();
            }
            mp.setRemainingSessions(remaining);
            if (remaining == 0) {
                mp.setStatus(SubscriptionStatus.EXPIRED);
            }
            memberPackageRepository.save(mp);
            createLog(member, mp, CheckInStatus.SUCCESS, String.format("Đang tập — Còn lại %d lượt.", remaining));
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message(String.format("Đang tập — Còn lại %d lượt.", remaining))
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .packageEndDate(mp.getEndDate())
                    .build();
        }

        if (mp.getGymPackage().getPackageType() == PackageType.PT_SESSION) {
            int remaining = mp.getRemainingSessions() != null ? mp.getRemainingSessions() - 1 : -1;
            if (remaining < 0) {
                return CheckInResponseDTO.builder()
                        .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                        .message("Gói PT đã hết buổi tập")
                        .memberFullName(member.getFullName())
                        .packageName(mp.getGymPackage().getName())
                        .packageEndDate(mp.getEndDate())
                        .build();
            }
            mp.setRemainingSessions(remaining);
            if (remaining == 0) {
                mp.setStatus(SubscriptionStatus.EXPIRED);
            }
            memberPackageRepository.save(mp);
            
            // Tạo PtSessionLog để hiển thị trong lịch tập
            User ptUser = mp.getAssignedPt();
            if (ptUser != null) {
                PtSessionLog ptSessionLog = PtSessionLog.builder()
                        .memberPackage(mp)
                        .ptUser(ptUser)
                        .sessionDate(now)
                        .notes("Quét QR check-in")
                        .build();
                ptSessionLogRepository.save(ptSessionLog);
            }
            
            createLog(member, mp, CheckInStatus.SUCCESS, String.format("Đang tập PT — Còn lại %d buổi.", remaining));
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.SUCCESS)
                    .message(String.format("Đang tập PT — Còn lại %d buổi.", remaining))
                    .memberFullName(member.getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .packageEndDate(mp.getEndDate())
                    .build();
        }

        // For GYM_ACCESS packages, create new check-in log
        createLog(member, mp, CheckInStatus.SUCCESS, "Đang tập (Gói thời hạn).");
        return CheckInResponseDTO.builder()
                .status(CheckInStatus.SUCCESS)
                .message("Đã vào phòng tập")
                .memberFullName(member.getFullName())
                .packageName(mp.getGymPackage().getName())
                .packageEndDate(mp.getEndDate())
                .build();
    }

    @Transactional
    public CheckInResponseDTO performCheckoutBySubscription(Long memberPackageId) {
        OffsetDateTime now = OffsetDateTime.now();
        MemberPackage mp = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Không tìm thấy đăng ký gói."));
        Optional<CheckInLog> openLogOpt = checkInLogRepository
                .findTopByMemberPackageIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(memberPackageId);
        if (openLogOpt.isEmpty()) {
            return CheckInResponseDTO.builder()
                    .status(CheckInStatus.FAILED_NO_ACTIVE_PACKAGE)
                    .message("Không có ca đang mở")
                    .memberFullName(mp.getMember().getFullName())
                    .packageName(mp.getGymPackage().getName())
                    .build();
        }
        CheckInLog log = openLogOpt.get();
        log.setCheckOutTime(now);
        if (log.getCheckInTime() != null) {
            long seconds = java.time.Duration.between(log.getCheckInTime(), now).getSeconds();
            log.setSessionDurationSeconds(seconds);
        }
        checkInLogRepository.save(log);
        return CheckInResponseDTO.builder()
                .status(CheckInStatus.SUCCESS)
                .message("Đã ra về")
                .memberFullName(mp.getMember().getFullName())
                .packageName(mp.getGymPackage().getName())
                .packageEndDate(mp.getEndDate())
                .sessionDurationSeconds(log.getSessionDurationSeconds())
                .build();
    }

    @Transactional(readOnly = true)
    public java.util.Optional<CheckInLog> getActiveCheckInByMemberId(Long memberId) {
        return checkInLogRepository.findTopByMemberIdAndCheckOutTimeIsNullOrderByCheckInTimeDesc(memberId);
    }

    @Transactional(readOnly = true)
    public java.util.List<CheckInLog> getRecentLogs(org.springframework.data.domain.Pageable pageable) {
        return checkInLogRepository.findAll(pageable).getContent();
    }

    @Transactional(readOnly = true)
    public java.util.List<CheckInLog> getCheckInLogs(Long memberId, Long packageId, 
                                                       OffsetDateTime startDate, OffsetDateTime endDate,
                                                       org.springframework.data.domain.Pageable pageable) {
        // Set default date range if not provided (last 30 days)
        if (startDate == null) {
            startDate = OffsetDateTime.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = OffsetDateTime.now().plusDays(1);
        }
        
        if (memberId != null && packageId != null) {
            return checkInLogRepository.findByMemberIdAndMemberPackageIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                    memberId, packageId, startDate, endDate, pageable);
        } else if (memberId != null) {
            if (startDate != null || endDate != null) {
                return checkInLogRepository.findByMemberIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                        memberId, startDate, endDate, pageable);
            } else {
                return checkInLogRepository.findByMemberIdOrderByCheckInTimeDesc(memberId, pageable);
            }
        } else if (packageId != null) {
            if (startDate != null || endDate != null) {
                return checkInLogRepository.findByMemberPackageIdAndCheckInTimeBetweenOrderByCheckInTimeDesc(
                        packageId, startDate, endDate, pageable);
            } else {
                return checkInLogRepository.findByMemberPackageIdOrderByCheckInTimeDesc(packageId, pageable);
            }
        } else {
            if (startDate != null || endDate != null) {
                return checkInLogRepository.findByCheckInTimeBetweenOrderByCheckInTimeDesc(startDate, endDate, pageable);
            } else {
                return checkInLogRepository.findAll(pageable).getContent();
            }
        }
    }
}
