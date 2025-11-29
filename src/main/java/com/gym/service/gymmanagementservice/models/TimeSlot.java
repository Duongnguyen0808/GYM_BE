package com.gym.service.gymmanagementservice.models;

import java.time.LocalTime;

public enum TimeSlot {
    MORNING("Sáng", LocalTime.of(9, 0), LocalTime.of(11, 0)),           // 9h - 11h
    AFTERNOON_1("Chiều 1", LocalTime.of(13, 0), LocalTime.of(15, 0)),   // 13h - 15h
    AFTERNOON_2("Chiều 2", LocalTime.of(16, 0), LocalTime.of(18, 0)),   // 16h - 18h
    EVENING("Tối", LocalTime.of(19, 0), LocalTime.of(21, 0));           // 19h - 21h
    
    private final String displayName;
    private final LocalTime startTime;
    private final LocalTime endTime;
    
    TimeSlot(String displayName, LocalTime startTime, LocalTime endTime) {
        this.displayName = displayName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public LocalTime getStartTime() {
        return startTime;
    }
    
    public LocalTime getEndTime() {
        return endTime;
    }
}














