package com.hopesapms.app.dto;

import lombok.Data;
import java.util.List;

@Data
public class GradebookDTO {
    private Long courseOfferingId; 
    private String courseName;
    private String sectionName;
    private String semesterName;
    private List<AssessmentSummaryDTO> assessmentSummaries;
    private List<StudentGradeRowDTO> studentGrades;
}