package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.PackageRequestDTO;
import com.gym.service.gymmanagementservice.dtos.PackageResponseDTO;
import com.gym.service.gymmanagementservice.models.GymPackage;
import com.gym.service.gymmanagementservice.models.PackageType;
import com.gym.service.gymmanagementservice.repositories.GymPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PackageService {

    private final GymPackageRepository gymPackageRepository;

    @Transactional
    public PackageResponseDTO createPackage(PackageRequestDTO request) {
        gymPackageRepository.findByName(request.getName()).ifPresent(p -> {
            throw new IllegalArgumentException("Tên gói tập đã tồn tại.");
        });

        validatePackageRequest(request);

        GymPackage newGymPackage = GymPackage.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .packageType(request.getPackageType())
                .durationDays(request.getDurationDays())
                .numberOfSessions(request.getNumberOfSessions())
                .startTimeLimit(request.getStartTimeLimit())
                .endTimeLimit(request.getEndTimeLimit())
                .isActive(true)
                .build();

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
                .collect(Collectors.toList());
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

        existingGymPackage.setName(request.getName());
        existingGymPackage.setDescription(request.getDescription());
        existingGymPackage.setPrice(request.getPrice());
        existingGymPackage.setPackageType(request.getPackageType());
        existingGymPackage.setDurationDays(request.getDurationDays());
        existingGymPackage.setNumberOfSessions(request.getNumberOfSessions());
        existingGymPackage.setStartTimeLimit(request.getStartTimeLimit());
        existingGymPackage.setEndTimeLimit(request.getEndTimeLimit());

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
                if (request.getDurationDays() == null || request.getDurationDays() <= 0) {
                    throw new IllegalArgumentException("Gói GYM_ACCESS phải có thời hạn (số ngày) lớn hơn 0.");
                }
                if (request.getNumberOfSessions() != null) {
                    throw new IllegalArgumentException("Gói GYM_ACCESS không yêu cầu số buổi (numberOfSessions).");
                }
                break;
            case PT_SESSION:
                if (request.getNumberOfSessions() == null || request.getNumberOfSessions() <= 0) {
                    throw new IllegalArgumentException("Gói PT_SESSION phải có số buổi (numberOfSessions) lớn hơn 0.");
                }
                if (request.getDurationDays() != null) {
                    throw new IllegalArgumentException("Gói PT_SESSION (hiện tại) không yêu cầu thời hạn (durationDays).");
                }
                if (startTime != null || endTime != null) {
                    throw new IllegalArgumentException("Không thể áp dụng giới hạn giờ (Off-Peak) cho gói PT_SESSION.");
                }
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
