package com.hopesapms.app.modules.grading.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

import com.hopesapms.app.modules.score.dto.StudentGradeRowDTO;

@Data
public class GradebookDTO {
    private Integer courseId;
    private String courseName;
    private String sectionName;

    private String semesterStatus;

    private List<AssessmentColumnDTO> columns;

    private List<StudentGradeRowDTO> rows;

    @Data
    public static class AssessmentColumnDTO {
        private Integer assessmentId;
        private String title;
        private Double maxScore;
        private Double weight;
    }

    @Data
    public static class StudentGradeRowDTO {
        private Integer enrollmentId;
        private String studentIdString;
        private String fullName;
        private Map<Integer, Double> scores;
    }
}