package com.hopesapms.app.modules.courseoffering.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; 

import com.hopesapms.app.modules.schedule.dto.ScheduleSlotDTO;

@Data
public class CourseOfferingResponseDTO {
    private Long id;
    private String status;

    private List<ScheduleSlotDTO> scheduleSlots;

    private Integer courseId;
    private String courseCode;
    private String courseTitle;
    private Double creditHour;  
    private Integer contactHours;
    private List<Integer> yearLevels;

    private Long semesterId;
    private String semesterName;

    private Long instructorId;
    private String instructorName;

    private String departmentName;

    
    private Integer sectionId;
    private String sectionName;
    private Integer sectionYearLevel;

    private LocalDateTime createdAt;
}