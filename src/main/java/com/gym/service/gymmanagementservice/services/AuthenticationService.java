package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.dtos.*;
import com.gym.service.gymmanagementservice.models.Member;
import com.gym.service.gymmanagementservice.models.Role;
import com.gym.service.gymmanagementservice.models.User;
import com.gym.service.gymmanagementservice.repositories.MemberRepository;
import com.gym.service.gymmanagementservice.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // <-- IMPORT MỚI
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final Random random = new Random();

    /**
     * MỚI: Hàm tạo OTP 6 số
     */
    private String generateOtp() {
        return String.format("%06d", random.nextInt(999999));
    }

    @Transactional
    public SignUpResponse signup(SignUpRequest request) {
        // Kiểm tra bằng SĐT
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
        }
        // (Giữ lại nếu vẫn muốn email là duy nhất)
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }

        String otp = generateOtp(); // Tạo OTP
        OffsetDateTime otpExpiry = OffsetDateTime.now().plusMinutes(10); // OTP hết hạn sau 10p

        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail()) // Lưu email (nếu có)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STAFF)
                .enabled(false)
                .verificationCode(otp)
                .verificationCodeExpiry(otpExpiry)
                .build();

        userRepository.save(user);

        // Mô phỏng việc gửi OTP bằng cách log ra console
        log.info("--- OTP MÔ PHỎNG (DEMO) ---");
        log.info("OTP cho SĐT {}: {}", request.getPhoneNumber(), otp);
        log.info("----------------------------");

        // Trả về DTO mới, chứa OTP để Flutter có thể demo
        return SignUpResponse.builder()
                .message("Đăng ký thành công! Vui lòng xác thực OTP (đã gửi mô phỏng).")
                .otpForDemo(otp) // Chỉ dùng cho demo
                .build();
    }

    // --- HÀM MỚI: Đăng ký cho Hội viên ---
    @Transactional
    public SignUpResponse memberSignup(MemberSignUpRequest request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
        }

        String otp = generateOtp();
        OffsetDateTime otpExpiry = OffsetDateTime.now().plusMinutes(10);

        // 1. Tạo bản ghi User (để đăng nhập)
        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                // (Email có thể null nếu hội viên không nhập)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.MEMBER) // <-- VAI TRÒ MEMBER
                .enabled(false) // Chờ OTP
                .verificationCode(otp)
                .verificationCodeExpiry(otpExpiry)
                .build();

        // 2. Tạo bản ghi Member (hồ sơ gym)
        Member member = Member.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .barcode(request.getPhoneNumber()) // Dùng SĐT làm barcode
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .build();

        // 3. Liên kết hai chiều
        user.setMemberProfile(member);
        member.setUserAccount(user);

        // 4. Lưu User (Member sẽ được lưu theo nhờ cascade = CascadeType.ALL)
        userRepository.save(user);

        log.info("--- OTP MÔ PHỎNG (MEMBER) ---");
        log.info("OTP cho SĐT {}: {}", request.getPhoneNumber(), otp);
        log.info("----------------------------");

        return SignUpResponse.builder()
                .message("Đăng ký hội viên thành công! Vui lòng xác thực OTP.")
                .otpForDemo(otp)
                .build();
    }

    /**
     * MỚI: Hàm Admin dùng để tạo tài khoản Staff/PT/Admin
     * Kích hoạt ngay lập tức.
     */
    @Transactional
    public void createStaffAccount(AdminCreateUserRequestDTO request) {
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("Số điện thoại đã tồn tại.");
        }
        if (request.getEmail() != null && !request.getEmail().isEmpty() && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại.");
        }

        // Không cho phép tạo Role MEMBER ở đây
        if (request.getRole() == Role.MEMBER) {
            throw new IllegalArgumentException("Không thể tạo tài khoản MEMBER từ giao diện này.");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole()) // Lấy Role từ DTO
                .enabled(true) // Kích hoạt ngay
                .verificationCode(null)
                .verificationCodeExpiry(null)
                .build();

        userRepository.save(user);
        log.info("Tài khoản nhân viên (SĐT: {}) đã được Admin tạo và kích hoạt.", request.getPhoneNumber());
    }

    /**
     * MỚI: Hàm xác thực OTP
     */
    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalStateException("Số điện thoại không tồn tại."));

        if (user.isEnabled()) {
            return "Tài khoản này đã được kích hoạt trước đó.";
        }

        if (user.getVerificationCode() == null || user.getVerificationCodeExpiry() == null) {
            throw new IllegalStateException("Tài khoản không ở trạng thái chờ xác thực.");
        }

        if (user.getVerificationCodeExpiry().isBefore(OffsetDateTime.now())) {
            // (Bạn có thể thêm logic gửi lại OTP ở đây)
            throw new IllegalStateException("OTP đã hết hạn. Vui lòng thử đăng ký lại.");
        }

        if (!user.getVerificationCode().equals(request.getOtp())) {
            throw new IllegalStateException("Mã OTP không chính xác.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        userRepository.save(user);

        return "Tài khoản đã được kích hoạt thành công. Bạn có thể đăng nhập ngay bây giờ.";
    }

    public JwtAuthenticationResponse signin(SignInRequest request) {
        // Đăng nhập bằng SĐT
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getPhoneNumber(), request.getPassword()));

        // Tìm bằng SĐT
        User user = userRepository.findByPhoneNumber(request.getPhoneNumber())
                .orElseThrow(() -> new IllegalArgumentException("SĐT hoặc mật khẩu không hợp lệ."));

        // (Kiểm tra thêm nếu cần)
        if (!user.isEnabled()) {
            throw new IllegalStateException("Tài khoản chưa được kích hoạt. Vui lòng xác thực OTP.");
        }
        if (user.isLocked()) {
            throw new IllegalStateException("Tài khoản đã bị khóa.");
        }

        String jwt = jwtService.generateToken(user); // Tạo token (vẫn dùng SĐT làm subject)
        return JwtAuthenticationResponse.builder().token(jwt).build();
    }

    public UserResponseDTO getMyProfile() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserPhoneNumber = userDetails.getUsername(); // Đây là SĐT

        User user = userRepository.findByPhoneNumber(currentUserPhoneNumber)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));

        return UserResponseDTO.fromUser(user);
    }

    public void changePassword(ChangePasswordRequest request) {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String currentUserPhoneNumber = userDetails.getUsername(); // Đây là SĐT

        User user = userRepository.findByPhoneNumber(currentUserPhoneNumber)
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng."));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalStateException("Mật khẩu hiện tại không đúng.");
        }

        if (!request.getNewPassword().equals(request.getConfirmationPassword())) {
            throw new IllegalStateException("Mật khẩu mới không khớp với mật khẩu xác nhận.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User getCurrentAuthenticatedUser() {
        String phoneNumber = SecurityContextHolder.getContext().getAuthentication().getName(); // Đây là SĐT
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng."));
    }
}