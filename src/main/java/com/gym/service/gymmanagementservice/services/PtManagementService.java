package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.PtSessionLogRepository;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PtManagementService {

    private final MemberPackageRepository memberPackageRepository;
    private final PtSessionLogRepository ptSessionLogRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Lấy thống kê tổng quan cho một PT
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPtStatistics(Long ptId) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);

        // 1. Số học viên đang có gói PT active (query trực tiếp từ database để lấy dữ liệu mới nhất)
        List<MemberPackage> activePackages = memberPackageRepository
                .findByAssignedPtIdAndStatusAndGymPackage_PackageType(
                        ptId, 
                        SubscriptionStatus.ACTIVE, 
                        PackageType.PT_SESSION);

        long activeStudents = activePackages.stream()
                .map(mp -> mp.getMember().getId())
                .distinct()
                .count();

        // 2. Tổng số học viên từ trước đến nay (lấy tất cả package PT của PT này, không phân biệt status)
        Set<Long> allStudentIds = memberPackageRepository.findAll().stream()
                .filter(mp -> mp.getAssignedPt() != null 
                        && mp.getAssignedPt().getId().equals(ptId)
                        && mp.getGymPackage().getPackageType() == PackageType.PT_SESSION)
                .map(mp -> mp.getMember().getId())
                .collect(Collectors.toSet());
        long totalStudents = allStudentIds.size();

        // 3. Tổng số buổi tập đã dạy
        List<PtSessionLog> allSessions = ptSessionLogRepository.findByPtUserId(ptId);
        long totalSessions = allSessions.size();

        // 4. Số buổi tập trong tháng này
        long sessionsThisMonth = allSessions.stream()
                .filter(session -> session.getSessionDate().isAfter(startOfMonth) 
                        || session.getSessionDate().isEqual(startOfMonth))
                .count();

        // 5. Tổng số buổi còn lại của tất cả học viên
        int totalRemainingSessions = activePackages.stream()
                .mapToInt(mp -> mp.getRemainingSessions() != null ? mp.getRemainingSessions() : 0)
                .sum();

        // 6. Doanh thu từ gói PT (từ transactions)
        List<Transaction> ptTransactions = transactionRepository.findAll().stream()
                .filter(tx -> tx.getMemberPackage() != null
                        && tx.getMemberPackage().getAssignedPt() != null
                        && tx.getMemberPackage().getAssignedPt().getId().equals(ptId)
                        && tx.getMemberPackage().getGymPackage().getPackageType() == PackageType.PT_SESSION
                        && tx.getStatus() == TransactionStatus.COMPLETED
                        && (tx.getKind() == TransactionKind.SUBSCRIPTION_NEW 
                                || tx.getKind() == TransactionKind.SUBSCRIPTION_RENEW))
                .collect(Collectors.toList());

        java.math.BigDecimal totalRevenue = ptTransactions.stream()
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 7. Doanh thu tháng này
        java.math.BigDecimal revenueThisMonth = ptTransactions.stream()
                .filter(tx -> tx.getTransactionDate().isAfter(startOfMonth) 
                        || tx.getTransactionDate().isEqual(startOfMonth))
                .map(Transaction::getAmount)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        // 8. Danh sách học viên đang active
        List<Map<String, Object>> activeStudentsList = activePackages.stream()
                .map(mp -> {
                    Map<String, Object> studentInfo = new HashMap<>();
                    studentInfo.put("memberId", mp.getMember().getId());
                    studentInfo.put("memberName", mp.getMember().getFullName());
                    studentInfo.put("phoneNumber", mp.getMember().getPhoneNumber());
                    studentInfo.put("packageName", mp.getGymPackage().getName());
                    studentInfo.put("remainingSessions", mp.getRemainingSessions());
                    studentInfo.put("startDate", mp.getStartDate());
                    studentInfo.put("endDate", mp.getEndDate());
                    studentInfo.put("packageId", mp.getId());
                    // Thêm thời gian tập (timeSlot)
                    if (mp.getTimeSlot() != null) {
                        studentInfo.put("timeSlot", mp.getTimeSlot().name());
                        studentInfo.put("timeSlotDisplay", mp.getTimeSlot().getDisplayName() + " (" + 
                                mp.getTimeSlot().getStartTime() + " - " + mp.getTimeSlot().getEndTime() + ")");
                    } else {
                        studentInfo.put("timeSlot", null);
                        studentInfo.put("timeSlotDisplay", "-");
                    }
                    // Thêm thời hạn gói tập (durationMonths)
                    if (mp.getGymPackage().getDurationMonths() != null && mp.getGymPackage().getDurationMonths() > 0) {
                        studentInfo.put("durationMonths", mp.getGymPackage().getDurationMonths());
                        studentInfo.put("durationDisplay", mp.getGymPackage().getDurationMonths() + " tháng");
                    } else {
                        studentInfo.put("durationMonths", null);
                        studentInfo.put("durationDisplay", "-");
                    }
                    // Thêm các thứ trong tuần cho phép tập
                    studentInfo.put("allowedWeekdays", mp.getAllowedWeekdays());
                    // Format các thứ để hiển thị
                    if (mp.getAllowedWeekdays() != null && !mp.getAllowedWeekdays().isBlank()) {
                        String[] days = mp.getAllowedWeekdays().split(",");
                        java.util.List<String> formattedDays = java.util.Arrays.stream(days)
                                .map(String::trim)
                                .map(day -> {
                                    switch (day) {
                                        case "MON": return "Thứ 2";
                                        case "TUE": return "Thứ 3";
                                        case "WED": return "Thứ 4";
                                        case "THU": return "Thứ 5";
                                        case "FRI": return "Thứ 6";
                                        case "SAT": return "Thứ 7";
                                        case "SUN": return "CN";
                                        default: return day;
                                    }
                                })
                                .collect(Collectors.toList());
                        studentInfo.put("allowedWeekdaysDisplay", String.join(", ", formattedDays));
                    } else {
                        studentInfo.put("allowedWeekdaysDisplay", "-");
                    }
                    return studentInfo;
                })
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("activeStudents", activeStudents);
        stats.put("totalStudents", totalStudents);
        stats.put("totalSessions", totalSessions);
        stats.put("sessionsThisMonth", sessionsThisMonth);
        stats.put("totalRemainingSessions", totalRemainingSessions);
        stats.put("totalRevenue", totalRevenue);
        stats.put("revenueThisMonth", revenueThisMonth);
        stats.put("activeStudentsList", activeStudentsList);

        return stats;
    }

    /**
     * Lấy danh sách tất cả PT với thống kê cơ bản
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllPtsWithStats(List<UserResponseDTO> allPts) {
        return allPts.stream()
                .filter(pt -> pt.getRole() == Role.PT)
                .map(pt -> {
                    Map<String, Object> ptInfo = new HashMap<>();
                    ptInfo.put("pt", pt);
                    
                    // Thống kê nhanh (query trực tiếp từ database)
                    List<MemberPackage> activePackages = memberPackageRepository
                            .findByAssignedPtIdAndStatusAndGymPackage_PackageType(
                                    pt.getId(), 
                                    SubscriptionStatus.ACTIVE, 
                                    PackageType.PT_SESSION);
                    long activeStudents = activePackages.stream()
                            .map(mp -> mp.getMember().getId())
                            .distinct()
                            .count();
                    
                    long totalSessions = ptSessionLogRepository.findByPtUserId(pt.getId()).size();
                    
                    ptInfo.put("activeStudents", activeStudents);
                    ptInfo.put("totalSessions", totalSessions);
                    
                    return ptInfo;
                })
                .collect(Collectors.toList());
    }
}

