package com.hopesapms.app.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ScoreResponseDTO {
    private Integer id;
    private Integer enrollmentId;
    private Integer assessmentId;
    private BigDecimal scoreValue;
    private String recordedByUsername;
    private LocalDateTime recordedDate;
    private boolean isOverriden;
    private BigDecimal originalScoreValue;
    private Integer scoreId;
    private Integer studentId;
    private String studentFullName;
    private String assessmentName;
    private String finalGrade;
    private Integer courseId;
    private String courseTitle;
    private Integer sectionId;
    private String sectionName;

    public ScoreResponseDTO(
            Integer scoreId,
            Integer studentId,
            String studentFullName,
            Integer assessmentId,
            Integer enrollmentId,
            String assessmentName,
            BigDecimal scoreValue,
            String finalGrade,
            Integer courseId,
            String courseTitle,
            Integer sectionId,
            String sectionName) {
        this.scoreId = scoreId;
        this.studentId = studentId;
        this.studentFullName = studentFullName;
        this.assessmentId = assessmentId;
        this.enrollmentId = enrollmentId;
        this.assessmentName = assessmentName;
        this.scoreValue = scoreValue;
        this.finalGrade = finalGrade;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.sectionId = sectionId;
        this.sectionName = sectionName;
    }
}