package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ClassSessionResponseDTO {
    private Integer id;
    private Integer courseId;
    private String courseName;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String location;
    private Integer scheduledInstructorId;
    private String scheduledInstructorName;
}