package com.hopesapms.app.modules.schedule.dto;

import lombok.Data;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class ScheduleResponseDTO {
    private Long courseOfferingId;
    private String courseName;
    private String courseCode;
    private String sectionName;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String room;
    private String color; 
}