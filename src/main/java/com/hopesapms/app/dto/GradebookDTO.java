package com.hopesapms.app.dto;

import lombok.Data;
import java.util.List;

@Data
public class GradebookDTO {
    private Integer courseId;
    private String courseName;
    private List<AssessmentSummaryDTO> assessmentSummaries;
    private List<StudentGradeRowDTO> studentGrades;
}