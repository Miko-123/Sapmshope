package com.hopesapms.app.modules.courseoffering.dto;

import lombok.Data;
import com.hopesapms.app.modules.schedule.dto.ScheduleSlotDTO;

import java.util.List; // Import List

@Data
public class CourseOfferingListDTO {
    private Long id;
    private String status;

    // Course
    private Integer courseId;
    private String courseCode;
    private String courseTitle;
    private Double credits;

    // Semester
    private Long semesterId;
    private String semesterName;
    private Integer semesterYear;

    // Instructor
    private Integer instructorId;
    private String instructorName;

    // Section
    private Integer sectionId;
    private String sectionName;

    // Schedule
    private List<ScheduleSlotDTO> scheduleSlots;
}