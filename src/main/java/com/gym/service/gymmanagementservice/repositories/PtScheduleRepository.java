package com.gym.service.gymmanagementservice.repositories;

import com.gym.service.gymmanagementservice.models.PtSchedule;
import com.gym.service.gymmanagementservice.models.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PtScheduleRepository extends JpaRepository<PtSchedule, Long> {
    
    List<PtSchedule> findByPtId(Long ptId);
    
    List<PtSchedule> findByPtIdAndScheduleDateBetween(Long ptId, LocalDate startDate, LocalDate endDate);
    
    Optional<PtSchedule> findByPtIdAndScheduleDateAndTimeSlot(Long ptId, LocalDate scheduleDate, TimeSlot timeSlot);
    
    @Query("SELECT ps FROM PtSchedule ps WHERE ps.pt.id = :ptId AND ps.scheduleDate >= :startDate AND ps.isAvailable = true")
    List<PtSchedule> findAvailableSchedulesByPtIdAndDateFrom(@Param("ptId") Long ptId, @Param("startDate") LocalDate startDate);
    
    List<PtSchedule> findByPtIdAndScheduleDate(Long ptId, LocalDate scheduleDate);
    
    void deleteByPtIdAndScheduleDateBefore(Long ptId, LocalDate date);
}






