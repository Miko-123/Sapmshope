package com.hopesapms.app.dto;

import lombok.Data;

@Data
public class PrerequisiteResponseDTO {
    private Integer id;
    private CourseSummaryDTO course; // The main course
    private CourseSummaryDTO prerequisiteCourse; // The required course
    private String minGrade;
    private boolean isRequired;
}