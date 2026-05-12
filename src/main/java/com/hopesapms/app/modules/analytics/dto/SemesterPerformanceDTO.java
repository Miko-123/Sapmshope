package com.hopesapms.app.modules.analytics.dto;

import lombok.Data;
import java.util.List;

@Data
public class SemesterPerformanceDTO {
    private String semesterName; // We might add this later
    private Double semesterGPA;
    private Double cumulativeGPA;
    private Double totalCreditsAttempted;
    private Double totalCreditsEarned;
    private List<CoursePerformanceDTO> courses;
}