package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EnrollmentResponseDTO {
    
    private Integer enrollmentId;
    private LocalDate enrollmentDate;
    private String status;
    private String finalGrade;
    private boolean isAddStudent;
    private Long courseOfferingId;
    private String courseCode;
    private String courseTitle;
    private String semesterName;
    private String instructorName;
    private String sectionName;
}