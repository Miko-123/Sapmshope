package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EnrollmentResponseDTO {
    
    private Integer enrollmentId;
    private Integer courseId;
    private String courseCode;
    private String courseTitle;
    
    private LocalDate enrollmentDate;
    private String status; 
    private String finalGrade;
    private boolean isAddStudent; 
}