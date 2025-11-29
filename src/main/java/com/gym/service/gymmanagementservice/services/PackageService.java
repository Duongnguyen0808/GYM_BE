package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.GymPackageRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final GymPackageRepository gymPackageRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final UserRepository userRepository;

    @Transactional
    public PackageResponseDTO createPackage(PackageRequestDTO request) {
        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
        });

        validatePackageRequest(request);

        // Logic đặc biệt cho gói PER_VISIT: Tự động 60 ngày
        Integer durationDays = request.getDurationDays();
        if (request.getPackageType() == PackageType.PER_VISIT) {
            durationDays = 60; // Luôn set 60 ngày cho gói theo lượt
        }

        GymPackage newGymPackage = GymPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .packageType(request.getPackageType())
                .durationDays(durationDays)
                .durationMonths(request.getDurationMonths())
                .numberOfSessions(request.getNumberOfSessions())
                .sessionsPerWeek(request.getSessionsPerWeek())
                .unlimited(request.getUnlimited())
                .startTimeLimit(request.getStartTimeLimit())
                .endTimeLimit(request.getEndTimeLimit())
                .isActive(true)
                .hinhAnh(request.getImageUrl())
                .allowedWeekdays(request.getAllowedWeekdays())
                .build();

        // Xử lý assignedPt cho gói PT
        if (request.getPackageType() == PackageType.PT_SESSION && request.getAssignedPtId() != null) {
            User pt = userRepository.findById(request.getAssignedPtId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
            if (pt.getRole() != Role.PT) {
                throw new IllegalArgumentException("Người dùng (ID: " + pt.getId() + ") không phải là PT.");
            }
            newGymPackage.setAssignedPt(pt);
        }

        // Chuẩn hóa dữ liệu null dựa trên loại gói
        if (newGymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            newGymPackage.setNumberOfSessions(null);
        } else if (newGymPackage.getPackageType() == PackageType.PT_SESSION) {
            newGymPackage.setDurationDays(null);
            newGymPackage.setStartTimeLimit(null);
            newGymPackage.setEndTimeLimit(null);
        }

        GymPackage savedGymPackage = gymPackageRepository.save(newGymPackage);
        return PackageResponseDTO.fromPackage(savedGymPackage);
    }

    public List<PackageResponseDTO> getAllPackages() {
        return gymPackageRepository.findAll().stream()
                .map(PackageResponseDTO::fromPackage)
                .sorted((a, b) -> {
                    // Sắp xếp theo giá giảm dần (giá cao nhất ở trên)
                    java.math.BigDecimal priceA = a.getPrice() != null ? a.getPrice() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal priceB = b.getPrice() != null ? b.getPrice() : java.math.BigDecimal.ZERO;
                    return priceB.compareTo(priceA);
                })
                .collect(Collectors.toList());
    }

    public List<PackageResponseDTO> searchPackages(String q,
                                                   java.math.BigDecimal minPrice,
                                                   java.math.BigDecimal maxPrice,
                                                   Integer durationDays,
                                                   String type,
                                                   Boolean active) {
        final String qq = (q == null ? null : normalize(q));
        return gymPackageRepository.findAll().stream()
                .filter(p -> {
                    if (qq == null || qq.isBlank()) return true;
                    String text = normalize((p.getName() == null ? "" : p.getName()) + " " + (p.getDescription() == null ? "" : p.getDescription()));
                    String[] tokens = Arrays.stream(qq.split("\\s+")).filter(t -> !t.isBlank()).toArray(String[]::new);
                    for (String t : tokens) { if (!text.contains(t)) return false; }
                    return true;
                })
                .filter(p -> minPrice == null || (p.getPrice() != null && p.getPrice().compareTo(minPrice) >= 0))
                .filter(p -> maxPrice == null || (p.getPrice() != null && p.getPrice().compareTo(maxPrice) <= 0))
                .filter(p -> durationDays == null || (p.getDurationDays() != null && p.getDurationDays().equals(durationDays)))
                .filter(p -> type == null || p.getPackageType().name().equalsIgnoreCase(type))
                .filter(p -> active == null || p.isActive() == active)
                .map(pkg -> {
                    PackageResponseDTO dto = PackageResponseDTO.fromPackage(pkg);
                    long count = memberPackageRepository.countByGymPackage_IdAndStatus(pkg.getId(), com.gym.service.gymmanagementservice.models.SubscriptionStatus.ACTIVE);
                    dto.setActiveMemberCount((int) count);
                    return dto;
                })
                .sorted((a, b) -> {
                    // Sắp xếp theo giá giảm dần (giá cao nhất ở trên)
                    java.math.BigDecimal priceA = a.getPrice() != null ? a.getPrice() : java.math.BigDecimal.ZERO;
                    java.math.BigDecimal priceB = b.getPrice() != null ? b.getPrice() : java.math.BigDecimal.ZERO;
                    return priceB.compareTo(priceA);
                })
                .collect(Collectors.toList());
    }

    private String normalize(String s) {
        String lower = s.toLowerCase();
        String norm = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return norm.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    public PackageResponseDTO getPackageById(Long id) {
        GymPackage pkg = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        return PackageResponseDTO.fromPackage(pkg);
    }

    @Transactional
    public PackageResponseDTO updatePackage(Long id, PackageRequestDTO request) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            if (!p.getId().equals(id)) {
                throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
            }
        });

        validatePackageRequest(request);

        // Logic đặc biệt cho gói PER_VISIT: Tự động 60 ngày
        Integer durationDays = request.getDurationDays();
        if (request.getPackageType() == PackageType.PER_VISIT) {
            durationDays = 60; // Luôn set 60 ngày cho gói theo lượt
        }

        existingGymPackage.setName(request.getName());
        existingGymPackage.setDescription(request.getDescription());
        existingGymPackage.setPrice(request.getPrice());
        existingGymPackage.setPackageType(request.getPackageType());
        
        // Với GYM_ACCESS: Ưu tiên durationMonths, clear durationDays nếu có durationMonths (và ngược lại)
        if (request.getPackageType() == PackageType.GYM_ACCESS) {
            if (request.getDurationMonths() != null && request.getDurationMonths() > 0) {
                existingGymPackage.setDurationMonths(request.getDurationMonths());
                existingGymPackage.setDurationDays(null); // Clear durationDays khi dùng durationMonths
            } else if (request.getDurationDays() != null && request.getDurationDays() > 0) {
                existingGymPackage.setDurationDays(durationDays);
                existingGymPackage.setDurationMonths(null); // Clear durationMonths khi dùng durationDays
            } else {
                // Giữ nguyên giá trị cũ nếu không có giá trị mới
                // existingGymPackage.setDurationDays/existingGymPackage.setDurationMonths không thay đổi
            }
        } else {
            existingGymPackage.setDurationDays(durationDays);
            existingGymPackage.setDurationMonths(request.getDurationMonths());
        }
        existingGymPackage.setNumberOfSessions(request.getNumberOfSessions());
        existingGymPackage.setSessionsPerWeek(request.getSessionsPerWeek());
        existingGymPackage.setUnlimited(request.getUnlimited());
        existingGymPackage.setStartTimeLimit(request.getStartTimeLimit());
        existingGymPackage.setEndTimeLimit(request.getEndTimeLimit());
        existingGymPackage.setAllowedWeekdays(request.getAllowedWeekdays());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            existingGymPackage.setHinhAnh(request.getImageUrl());
        }

        // Xử lý assignedPt cho gói PT
        if (request.getPackageType() == PackageType.PT_SESSION && request.getAssignedPtId() != null) {
            User pt = userRepository.findById(request.getAssignedPtId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy PT với ID: " + request.getAssignedPtId()));
            if (pt.getRole() != Role.PT) {
                throw new IllegalArgumentException("Người dùng (ID: " + pt.getId() + ") không phải là PT.");
            }
            existingGymPackage.setAssignedPt(pt);
        } else if (request.getPackageType() == PackageType.PT_SESSION && request.getAssignedPtId() == null) {
            existingGymPackage.setAssignedPt(null);
        }

        // Chuẩn hóa dữ liệu null dựa trên loại gói
        if (existingGymPackage.getPackageType() == PackageType.GYM_ACCESS) {
            existingGymPackage.setNumberOfSessions(null);
        } else if (existingGymPackage.getPackageType() == PackageType.PT_SESSION) {
            existingGymPackage.setDurationDays(null);
            existingGymPackage.setStartTimeLimit(null);
            existingGymPackage.setEndTimeLimit(null);
        }

        GymPackage updatedGymPackage = gymPackageRepository.save(existingGymPackage);
        return PackageResponseDTO.fromPackage(updatedGymPackage);
    }

    @Transactional
    public void togglePackageStatus(Long id) {
        GymPackage existingGymPackage = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));

        existingGymPackage.setActive(!existingGymPackage.isActive());
        gymPackageRepository.save(existingGymPackage);
    }

    @Transactional
    public void deletePackage(Long id) {
        GymPackage pkg = gymPackageRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy gói tập với ID: " + id));
        boolean hasSubscriptions = memberPackageExistsByPackageId(id);
        if (hasSubscriptions) {
            throw new IllegalStateException("Không thể xóa gói vì đang được sử dụng bởi hội viên.");
        }
        gymPackageRepository.delete(pkg);
    }

    private boolean memberPackageExistsByPackageId(Long packageId) {
        return memberPackageRepository.existsByGymPackage_Id(packageId);
    }

    // Thêm hàm validate logic gói
    private void validatePackageRequest(PackageRequestDTO request) {

        LocalTime startTime = request.getStartTimeLimit();
        LocalTime endTime = request.getEndTimeLimit();

        // Kiểm tra logic khung giờ
        if (startTime != null && endTime != null) {
            if (startTime.isAfter(endTime) || startTime.equals(endTime)) {
                throw new IllegalArgumentException("Khung giờ bắt đầu phải sớm hơn khung giờ kết thúc.");
            }
        } else if (startTime != null || endTime != null) {
            throw new IllegalArgumentException("Phải cung cấp cả giờ bắt đầu và giờ kết thúc, hoặc để trống cả hai.");
        }

        switch (request.getPackageType()) {
            case GYM_ACCESS:
                // Gói GYM_ACCESS: Chấp nhận durationMonths (ưu tiên) hoặc durationDays (backward compatibility)
                // Nếu có cả durationMonths và durationDays, ưu tiên durationMonths và bỏ qua durationDays
                Integer finalDurationMonths = request.getDurationMonths();
                Integer finalDurationDays = request.getDurationDays();
                
                if (finalDurationMonths != null && finalDurationMonths > 0) {
                    // Nếu có durationMonths hợp lệ, bỏ qua durationDays
                    finalDurationDays = null;
                } else if (finalDurationDays != null && finalDurationDays > 0) {
                    // Nếu không có durationMonths nhưng có durationDays, giữ durationDays
                    // OK
                } else {
                    // Không có cả hai
                    throw new IllegalArgumentException("Gói GYM_ACCESS phải có thời hạn (số tháng hoặc số ngày) lớn hơn 0.");
                }
                
                // Cập nhật lại request để đồng bộ (đảm bảo không có cả hai)
                request.setDurationMonths(finalDurationMonths);
                request.setDurationDays(finalDurationDays);
                
                if (request.getNumberOfSessions() != null) {
                    throw new IllegalArgumentException("Gói GYM_ACCESS không yêu cầu số buổi (numberOfSessions).");
                }
                break;
            case PT_SESSION:
                if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
                    throw new IllegalArgumentException("Gói PT_SESSION phải có số buổi (numberOfSessions) lớn hơn 0.");
                }
                if (request.getDurationMonths() == null || request.getDurationMonths() <= 0) {
                    throw new IllegalArgumentException("Gói PT_SESSION phải có thời hạn (số tháng) lớn hơn 0.");
                }
                if (request.getDurationDays() != null) {
                    throw new IllegalArgumentException("Gói PT_SESSION không dùng durationDays, hãy dùng durationMonths.");
                }
                if (startTime != null || endTime != null) {
                    throw new IllegalArgumentException("Không thể áp dụng giới hạn giờ (Off-Peak) cho gói PT_SESSION.");
                }
                // Validate số buổi phù hợp với số tháng
                int sessions = request.getNumberOfSessions();
                int months = request.getDurationMonths();
                if (sessions == 30 && (months < 1 || months > 2)) {
                    throw new IllegalArgumentException("Gói 30 buổi chỉ có thể chọn 1 hoặc 2 tháng.");
                }
                if (sessions == 60 && (months < 2 || months > 3)) {
                    throw new IllegalArgumentException("Gói 60 buổi chỉ có thể chọn 2 hoặc 3 tháng.");
                }
                if (sessions != 30 && sessions != 60) {
                    // Cho phép các số buổi khác với validation linh hoạt hơn
                    if (sessions < 30 && months > 1) {
                        throw new IllegalArgumentException("Gói dưới 30 buổi chỉ nên chọn 1 tháng.");
                    }
                    if (sessions > 60 && months < 2) {
                        throw new IllegalArgumentException("Gói trên 60 buổi nên chọn ít nhất 2 tháng.");
                    }
                }
                // assignedPtId là tùy chọn, nhưng nếu có thì phải validate trong createPackage/updatePackage
                break;
            case PER_VISIT:
                if (request.getDurationDays() == null || request.getDurationDays() <= 0) {
                    throw new IllegalArgumentException("Gói PER_VISIT phải có thời hạn (số ngày) lớn hơn 0.");
                }
                if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
                    throw new IllegalArgumentException("Gói PER_VISIT phải có số lượt (numberOfSessions) lớn hơn 0.");
                }
                break;
            default:
                throw new IllegalArgumentException("Loại gói tập không xác định.");
        }
    }
}
