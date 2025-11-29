package com.gym.service.gymmanagementservice.services;

import com.gym.service.gymmanagementservice.models.*;
import com.gym.service.gymmanagementservice.repositories.MemberPackageRepository;
import com.gym.service.gymmanagementservice.repositories.PtBookingRepository;
import com.gym.service.gymmanagementservice.repositories.PtScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PtBookingService {

    private final PtBookingRepository ptBookingRepository;
    private final PtScheduleRepository ptScheduleRepository;
    private final MemberPackageRepository memberPackageRepository;
    private final AuthenticationService authenticationService;

    /**
     * Học viên đặt lịch tập với PT
     */
    @Transactional
    public PtBooking createBooking(Long memberPackageId, LocalDate bookingDate, TimeSlot timeSlot, String notes) {
        MemberPackage memberPackage = memberPackageRepository.findById(memberPackageId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy gói tập với ID: " + memberPackageId));

        // Kiểm tra gói tập là PT_SESSION
        if (memberPackage.getGymPackage().getPackageType() != PackageType.PT_SESSION) {
            throw new IllegalArgumentException("Chỉ gói PT mới có thể đặt lịch.");
        }

        // Kiểm tra gói tập còn active
        if (memberPackage.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new IllegalStateException("Gói tập không ở trạng thái hoạt động.");
        }

        // Kiểm tra còn buổi tập
        if (memberPackage.getRemainingSessions() == null || memberPackage.getRemainingSessions() <= 0) {
            throw new IllegalStateException("Gói tập đã hết buổi tập.");
        }

        User pt = memberPackage.getAssignedPt();
        if (pt == null) {
            throw new IllegalStateException("Gói tập chưa được gán PT.");
        }

        // Kiểm tra ngày đặt không được trong quá khứ
        if (bookingDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Không thể đặt lịch trong quá khứ.");
        }

        // Kiểm tra PT có available không
        PtSchedule schedule = ptScheduleRepository
                .findByPtIdAndScheduleDateAndTimeSlot(pt.getId(), bookingDate, timeSlot)
                .orElse(null);

        if (schedule != null && !schedule.getIsAvailable()) {
            throw new IllegalStateException("PT không available vào khung giờ này.");
        }

        // Kiểm tra đã có booking chưa
        boolean hasExistingBooking = ptBookingRepository
                .findByPtIdAndBookingDateAndTimeSlotAndStatusIn(
                        pt.getId(),
                        bookingDate,
                        timeSlot,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
                )
                .isPresent();

        if (hasExistingBooking) {
            throw new IllegalStateException("Khung giờ này đã được đặt bởi học viên khác.");
        }

        // Tạo booking
        PtBooking booking = PtBooking.builder()
                .memberPackage(memberPackage)
                .pt(pt)
                .bookingDate(bookingDate)
                .timeSlot(timeSlot)
                .status(BookingStatus.PENDING)
                .notes(notes)
                .build();

        return ptBookingRepository.save(booking);
    }

    /**
     * PT xác nhận booking
     */
    @Transactional
    public PtBooking confirmBooking(Long bookingId) {
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy booking với ID: " + bookingId));

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if (!booking.getPt().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền xác nhận booking này.");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalStateException("Chỉ có thể xác nhận booking đang pending.");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(OffsetDateTime.now());

        return ptBookingRepository.save(booking);
    }

    /**
     * Hủy booking
     */
    @Transactional
    public PtBooking cancelBooking(Long bookingId, String reason, String cancelledBy) {
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy booking với ID: " + bookingId));

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        
        // Kiểm tra quyền
        boolean canCancel = false;
        if (currentUser.getRole() == Role.ADMIN) {
            canCancel = true;
        } else if (currentUser.getRole() == Role.PT && booking.getPt().getId().equals(currentUser.getId())) {
            canCancel = true;
        } else if (booking.getMemberPackage().getMember().getUserAccount() != null 
                && booking.getMemberPackage().getMember().getUserAccount().getId().equals(currentUser.getId())) {
            canCancel = true;
        }

        if (!canCancel) {
            throw new AccessDeniedException("Bạn không có quyền hủy booking này.");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new IllegalStateException("Không thể hủy booking đã hoàn thành.");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking này đã bị hủy rồi.");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledBy(cancelledBy);
        booking.setUpdatedAt(OffsetDateTime.now());

        return ptBookingRepository.save(booking);
    }

    /**
     * Đánh dấu booking đã hoàn thành
     */
    @Transactional
    public PtBooking completeBooking(Long bookingId, String notes) {
        PtBooking booking = ptBookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy booking với ID: " + bookingId));

        User currentUser = authenticationService.getCurrentAuthenticatedUser();
        if (!booking.getPt().getId().equals(currentUser.getId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Bạn không có quyền hoàn thành booking này.");
        }

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Chỉ có thể hoàn thành booking đã được xác nhận.");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        if (notes != null) {
            booking.setNotes(notes);
        }
        booking.setUpdatedAt(OffsetDateTime.now());

        return ptBookingRepository.save(booking);
    }

    /**
     * Lấy danh sách booking của PT
     */
    @Transactional(readOnly = true)
    public List<PtBooking> getPtBookings(Long ptId, LocalDate startDate, LocalDate endDate) {
        return ptBookingRepository.findByPtIdAndBookingDateBetween(ptId, startDate, endDate);
    }

    /**
     * Lấy danh sách booking của học viên
     */
    @Transactional(readOnly = true)
    public List<PtBooking> getMemberBookings(Long memberId) {
        return ptBookingRepository.findByMemberId(memberId);
    }

    /**
     * Lấy booking sắp tới của PT
     */
    @Transactional(readOnly = true)
    public List<PtBooking> getUpcomingPtBookings(Long ptId) {
        return ptBookingRepository.findUpcomingBookingsByPtId(
                ptId,
                LocalDate.now(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );
    }

    /**
     * Lấy booking sắp tới của học viên
     */
    @Transactional(readOnly = true)
    public List<PtBooking> getUpcomingMemberBookings(Long memberId) {
        return ptBookingRepository.findUpcomingBookingsByMemberId(
                memberId,
                LocalDate.now(),
                List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED)
        );
    }
}

