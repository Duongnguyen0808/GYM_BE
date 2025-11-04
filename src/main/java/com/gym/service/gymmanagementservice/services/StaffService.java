package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.AdminUpdateUserRequestDTO;
import com.gym.service.gymmanagementservice.dtos.UserResponseDTO;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException; // <-- IMPORT MỚI
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {

    private final UserRepository userRepository;

    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * MỚI: Hàm lấy thông tin 1 user
     */
    public UserResponseDTO getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));
        return UserResponseDTO.fromUser(user);
    }


    @Transactional
    public UserResponseDTO updateUserByAdmin(Long userId, AdminUpdateUserRequestDTO requestDTO) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho phép đổi role MEMBER
        if (user.getRole() == com.gym.service.gymmanagementservice.models.Role.MEMBER ||
                requestDTO.getRole() == com.gym.service.gymmanagementservice.models.Role.MEMBER) {
            throw new IllegalArgumentException("Không thể thay đổi vai trò HỘI VIÊN (MEMBER) tại đây.");
        }

        user.setFullName(requestDTO.getFullName());
        user.setRole(requestDTO.getRole());
        user.setLocked(requestDTO.isLocked());

        User savedUser = userRepository.save(user);
        return UserResponseDTO.fromUser(savedUser);
    }

    /**
     * MỚI: Lấy danh sách tất cả các user có vai trò là PT
     */
    public List<UserResponseDTO> getAllPts() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRole() == com.gym.service.gymmanagementservice.models.Role.PT)
                .map(UserResponseDTO::fromUser)
                .collect(Collectors.toList());
    }

    /**
     * MỚI: Khóa hoặc Mở khóa tài khoản nhân viên
     */
    @Transactional
    public void toggleUserLockStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người dùng với ID: " + userId));

        // Không cho phép khóa/mở khóa HỘI VIÊN từ đây
        if (user.getRole() == com.gym.service.gymmanagementservice.models.Role.MEMBER) {
            throw new IllegalArgumentException("Không thể quản lý trạng thái tài khoản HỘI VIÊN tại đây.");
        }

        user.setLocked(!user.isLocked()); // Đảo ngược trạng thái khóa
        userRepository.save(user);
    }
}