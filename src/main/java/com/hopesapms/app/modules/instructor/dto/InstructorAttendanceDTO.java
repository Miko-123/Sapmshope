package com.hopesapms.app.modules.instructor.dto;

import lombok.Data;
import java.time.LocalTime;

@Data
public class InstructorAttendanceDTO {
    private Long courseOfferingId;
    private String courseTitle;
    private String courseCode;
    private String instructorName;
    private String sectionName;
    private String roomNumber;

    private LocalTime startTime;
    private LocalTime endTime;

    private String status;
    private Long sessionId;
}