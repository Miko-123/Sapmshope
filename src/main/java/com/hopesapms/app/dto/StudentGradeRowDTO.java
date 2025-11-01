package com.hopesapms.app.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class StudentGradeRowDTO {
    private Integer enrollmentId;
    private Integer studentUserId;
    private String studentName;
    private String studentId; // The University-assigned student ID
    private BigDecimal finalPercentage; // The calculated weighted percentage
    private String letterGrade;
}