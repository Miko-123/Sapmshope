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
    private Double credits;
    private Integer yearLevel;

    // Semester Details
    private Long semesterId;
    private String semesterName;

    // Instructor Details
    private Integer instructorId;
    private String instructorName;

    // Section Details
    private Integer sectionId;
    private String sectionName;

    private LocalDateTime createdAt;
}