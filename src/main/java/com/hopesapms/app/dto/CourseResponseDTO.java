package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CourseResponseDTO {
    private Integer id; 
    private String title;
    private String courseCode;
    private Double credits;
    private String description;
    
    private Long programId;
    private String programName;
    private Long departmentId;
    private String departmentName;

    private Long academicSemesterId;
    private String academicSemesterName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}