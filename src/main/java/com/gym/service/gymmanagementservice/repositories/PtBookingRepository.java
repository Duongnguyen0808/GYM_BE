package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.BookingStatus;
import com.gym.service.gymmanagementservice.models.PtBooking;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PtBookingRepository extends JpaRepository<PtBooking, Long> {
    
    List<PtBooking> findByPtId(Long ptId);
    
    List<PtBooking> findByMemberPackageId(Long memberPackageId);
    
    List<PtBooking> findByPtIdAndBookingDateBetween(Long ptId, LocalDate startDate, LocalDate endDate);
    
    List<PtBooking> findByPtIdAndBookingDate(Long ptId, LocalDate bookingDate);
    
    Optional<PtBooking> findByPtIdAndBookingDateAndTimeSlotAndStatusIn(
        Long ptId, 
        LocalDate bookingDate, 
        TimeSlot timeSlot,
        List<BookingStatus> statuses
    );
    
    @Query("SELECT b FROM PtBooking b WHERE b.memberPackage.member.id = :memberId ORDER BY b.bookingDate DESC, b.timeSlot")
    List<PtBooking> findByMemberId(@Param("memberId") Long memberId);
    
    @Query("SELECT b FROM PtBooking b WHERE b.pt.id = :ptId AND b.bookingDate >= :startDate AND b.status IN :statuses ORDER BY b.bookingDate, b.timeSlot")
    List<PtBooking> findUpcomingBookingsByPtId(
        @Param("ptId") Long ptId, 
        @Param("startDate") LocalDate startDate,
        @Param("statuses") List<BookingStatus> statuses
    );
    
    @Query("SELECT b FROM PtBooking b WHERE b.memberPackage.member.id = :memberId AND b.bookingDate >= :startDate AND b.status IN :statuses ORDER BY b.bookingDate, b.timeSlot")
    List<PtBooking> findUpcomingBookingsByMemberId(
        @Param("memberId") Long memberId,
        @Param("startDate") LocalDate startDate,
        @Param("statuses") List<BookingStatus> statuses
    );
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM pt_bookings WHERE member_package_id IN (SELECT id FROM member_packages WHERE member_id = :memberId)", nativeQuery = true)
    void deleteByMemberId(@Param("memberId") Long memberId);
}

