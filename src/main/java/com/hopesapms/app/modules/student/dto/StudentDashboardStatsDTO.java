package com.hopesapms.app.modules.student.dto;

import lombok.Data;

@Data
public class StudentDashboardStatsDTO {
    private Double semesterGPA;
    private Double cumulativeGPA;
    private Integer totalCreditsEarned;
    private Integer currentSemesterCredits;
}