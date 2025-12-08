package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List; // Import List

@Data
public class CourseOfferingResponseDTO {
    private Long id;
    private String status;

    // ✅ REPLACING 'day', 'time', 'room'
    private List<ScheduleSlotDTO> scheduleSlots;

    // Course Details
    private Integer courseId;
    private String courseCode;
    private String courseTitle;
    private Double creditHour;  // Updated: Use creditHour instead of credits for consistency with entity
    private Integer contactHours;
    private List<Integer> yearLevels;


    // Semester Details
    private Long semesterId;
    private String semesterName;

    // Instructor Details
    private Long instructorId;
    private String instructorName;

    private String departmentName;

    // Section Details
    private Integer sectionId;
    private String sectionName;
    private Integer sectionYearLevel;

    private LocalDateTime createdAt;
}