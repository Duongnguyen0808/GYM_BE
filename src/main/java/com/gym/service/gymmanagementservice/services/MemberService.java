package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.MemberRequestDTO;
import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
// Bỏ import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponseDTO createMember(MemberRequestDTO request) {

        // Đổi tên biến để rõ ràng hơn
        String phoneNumber = request.getPhoneNumber();
        String email = request.getEmail();

        // Kiểm tra SĐT trên bảng User (vì SĐT là unique)
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký.");
        }
        if (email != null && !email.isEmpty() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được đăng ký.");
        }

        // TẠO CẢ USER VÀ MEMBER
        // 1. Tạo User
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(phoneNumber)
                .email(email)
                // Staff tạo -> kích hoạt luôn, đặt mật khẩu mặc định (SĐT)
                .password(passwordEncoder.encode(phoneNumber))
                .role(Role.MEMBER)
                .enabled(true)
                .build();

        // 2. Tạo Member
        Member member = Member.builder()
                .fullName(request.getFullName())
                .phoneNumber(phoneNumber)
                .email(email)
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .barcode(phoneNumber) // Dùng SĐT làm barcode
                .build();

        // 3. Liên kết 2 chiều
        user.setMemberProfile(member);
        member.setUserAccount(user);

        // 4. Lưu User (Member sẽ tự lưu)
        userRepository.save(user);

        return MemberResponseDTO.fromMember(member);
    }

    public List<MemberResponseDTO> getAllMembers() {
        return memberRepository.findAll().stream()
                .map(MemberResponseDTO::fromMember)
                .collect(Collectors.toList());
    }

    public MemberResponseDTO getMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));
        return MemberResponseDTO.fromMember(member);
    }

    // Cập nhật thông tin hội viên
    @Transactional
    public MemberResponseDTO updateMember(Long memberId, MemberRequestDTO request) {
        Member existingMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));

        // Lấy User liên kết
        User userAccount = existingMember.getUserAccount();
        if (userAccount == null) {
            throw new IllegalStateException("Hội viên này không có tài khoản (User) liên kết.");
        }

        // Kiểm tra SĐT (trên bảng User)
        userRepository.findByPhoneNumber(request.getPhoneNumber()).ifPresent(user -> {
            if (!Objects.equals(user.getId(), userAccount.getId())) {
                throw new IllegalArgumentException("Số điện thoại đã được đăng ký bởi người khác.");
            }
        });

        // Kiểm tra Email (trên bảng User)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            // (Lưu ý: hàm findByEmail không có trên UserRepository, bạn nên thêm nó vào)
            // userRepository.findByEmail(request.getEmail()).ifPresent(user -> { ... });
        }

        // Cập nhật cả User và Member
        userAccount.setFullName(request.getFullName());
        userAccount.setPhoneNumber(request.getPhoneNumber());
        userAccount.setEmail(request.getEmail());

        existingMember.setFullName(request.getFullName());
        existingMember.setPhoneNumber(request.getPhoneNumber());
        existingMember.setEmail(request.getEmail());
        existingMember.setBirthDate(request.getBirthDate());
        existingMember.setAddress(request.getAddress());
        existingMember.setBarcode(request.getPhoneNumber());

        // Lưu User (MemberRepository sẽ tự lưu Member do liên kết)
        userRepository.save(userAccount);

        return MemberResponseDTO.fromMember(existingMember);
    }
}