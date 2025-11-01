package com.hopesapms.app.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CoursePerformanceDTO {
    private String courseCode;
    private String courseTitle;
    private Double credits;
    private BigDecimal finalPercentage; // e.g., 87.50
    private String letterGrade; // e.g., "B+"
    private Double gradePoint; // e.g., 3.33
}