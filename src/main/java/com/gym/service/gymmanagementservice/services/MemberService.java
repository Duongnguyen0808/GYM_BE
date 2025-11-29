package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.MemberRequestDTO;
import com.gym.service.gymmanagementservice.dtos.MemberResponseDTO;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.SaleRepository;
import com.gym.service.gymmanagementservice.repositories.TransactionRepository;
import com.gym.service.gymmanagementservice.repositories.CheckInLogRepository;
import com.gym.service.gymmanagementservice.repositories.PtSessionLogRepository;
import com.gym.service.gymmanagementservice.repositories.PtBookingRepository;
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
    private final MemberPackageRepository memberPackageRepository;
    private final SaleRepository saleRepository;
    private final TransactionRepository transactionRepository;
    private final CheckInLogRepository checkInLogRepository;
    private final PtSessionLogRepository ptSessionLogRepository;
    private final PtBookingRepository ptBookingRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public MemberResponseDTO createMember(MemberRequestDTO request) {

        // Đổi tên biến để rõ ràng hơn
        String phoneNumber = request.getPhoneNumber();

        // Kiểm tra SĐT trên bảng User (vì SĐT là unique)
        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Số điện thoại đã được đăng ký.");
        }

        // TẠO CẢ USER VÀ MEMBER
        // 1. Tạo User
        // Nếu có password từ request, dùng nó; nếu không, dùng SĐT làm mật khẩu mặc định
        String passwordToUse = (request.getPassword() != null && !request.getPassword().trim().isEmpty()) 
                ? request.getPassword() 
                : phoneNumber;
        
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(phoneNumber)
                .email(null) // Email không cần nữa
                // Staff tạo -> kích hoạt luôn
                .password(passwordEncoder.encode(passwordToUse))
                .role(Role.MEMBER)
                .enabled(true)
                .build();

        // 2. Tạo Member
        Member member = Member.builder()
                .fullName(request.getFullName())
                .phoneNumber(phoneNumber)
                .email(null) // Email không cần nữa
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .barcode(phoneNumber) // Dùng SĐT làm barcode
                .avatarUrl(null)
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

        // Cập nhật cả User và Member
        userAccount.setFullName(request.getFullName());
        userAccount.setPhoneNumber(request.getPhoneNumber());
        userAccount.setEmail(null); // Email không cần nữa

        existingMember.setFullName(request.getFullName());
        existingMember.setPhoneNumber(request.getPhoneNumber());
        existingMember.setEmail(null); // Email không cần nữa
        existingMember.setBirthDate(request.getBirthDate());
        existingMember.setAddress(request.getAddress());
        existingMember.setBarcode(request.getPhoneNumber());

        // Avatar giữ nguyên nếu không có thay đổi (upload xử lý ở Controller)

        // Lưu User (MemberRepository sẽ tự lưu Member do liên kết)
        userRepository.save(userAccount);

        return MemberResponseDTO.fromMember(existingMember);
    }

    @Transactional
    public void updateAvatar(Long memberId, String avatarUrl) {
        if (avatarUrl == null || avatarUrl.isBlank()) return;
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId));
        member.setAvatarUrl(avatarUrl);
        memberRepository.save(member);
    }

    @Transactional
    public void deleteMember(Long memberId) {
        // Kiểm tra member có tồn tại không (không load vào session)
        if (!memberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("Không tìm thấy hội viên với ID: " + memberId);
        }
        
        // Lấy user ID từ database bằng native query (không load Member entity vào session)
        Long userId = memberRepository.findUserIdByMemberId(memberId).orElse(null);
        
        // Đếm số gói đang hoạt động (ACTIVE, PENDING, FROZEN) - dùng native query
        long activePkg = memberPackageRepository.findByMemberId(memberId).stream()
                .filter(mp -> mp.getStatus() == com.gym.service.gymmanagementservice.models.SubscriptionStatus.ACTIVE
                        || mp.getStatus() == com.gym.service.gymmanagementservice.models.SubscriptionStatus.PENDING
                        || mp.getStatus() == com.gym.service.gymmanagementservice.models.SubscriptionStatus.FROZEN)
                .count();
        
        // Kiểm tra gói đang hoạt động - nếu có thì không cho xóa
        if (activePkg > 0) {
            throw new IllegalStateException(
                String.format("Không thể xóa hội viên vì đang có %d gói đang hoạt động (ACTIVE/PENDING/FROZEN).", activePkg)
            );
        }

        // Xóa tất cả hóa đơn bán hàng của hội viên này (nếu có)
        java.util.List<com.gym.service.gymmanagementservice.models.Sale> sales = saleRepository.findByMember_Id(memberId);
        
        if (!sales.isEmpty()) {
            // Lấy danh sách sale IDs
            java.util.List<Long> saleIds = sales.stream()
                    .map(com.gym.service.gymmanagementservice.models.Sale::getId)
                    .collect(java.util.stream.Collectors.toList());
            
            // Xóa Transaction liên kết với các Sale (nếu có)
            // Dùng JPQL update để set sale = null trước
            for (Long saleId : saleIds) {
                transactionRepository.setSaleNullBySaleId(saleId);
            }
            transactionRepository.flush();
            
            // Sau đó xóa các Transaction (nếu có)
            for (Long saleId : saleIds) {
                java.util.List<com.gym.service.gymmanagementservice.models.Transaction> transactions = 
                    transactionRepository.findAllBySale_Id(saleId);
                for (com.gym.service.gymmanagementservice.models.Transaction transaction : transactions) {
                    transactionRepository.delete(transaction);
                }
            }
            
            // Xóa SaleDetail trước (do foreign key constraint)
            saleRepository.deleteSaleDetailsByMemberId(memberId);
            saleRepository.flush();
            
            // Xóa Sale trực tiếp bằng native query (tránh lỗi Hibernate entity state)
            saleRepository.deleteByMemberId(memberId);
            saleRepository.flush();
        }

        // Xóa PtSessionLog và PtBooking liên kết với MemberPackage trước
        ptSessionLogRepository.deleteByMemberId(memberId);
        ptSessionLogRepository.flush();
        
        ptBookingRepository.deleteByMemberId(memberId);
        ptBookingRepository.flush();
        
        // Xóa Transaction liên kết với MemberPackage
        transactionRepository.deleteByMemberId(memberId);
        transactionRepository.flush();
        
        // Xóa tất cả CheckInLog bằng native query (trước khi xóa MemberPackage)
        checkInLogRepository.deleteAllByMemberId(memberId);
        checkInLogRepository.flush();
        
        // Xóa tất cả MemberPackage (kể cả đã CANCELLED) bằng native query
        memberPackageRepository.deleteAllByMemberId(memberId);
        memberPackageRepository.flush();
        
        // Xóa member bằng native query để tránh lỗi Hibernate
        memberRepository.deleteByIdNative(memberId);
        memberRepository.flush();
        
        // Xóa user nếu có (bằng native query)
        if (userId != null) {
            userRepository.deleteById(userId);
            userRepository.flush();
        }
    }
}
