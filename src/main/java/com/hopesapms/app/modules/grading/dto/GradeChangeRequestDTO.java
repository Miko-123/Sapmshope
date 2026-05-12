package com.hopesapms.app.modules.grading.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class GradeChangeRequestDTO {
    private Long id;

    private Long enrollmentId;
    private String studentName;
    private String studentId;
    private String courseCode;

    private Integer assessmentId;
    private String assessmentName;

    private Double oldScore;
    private Double newScore;
    private String reason;
    private String status;

    private String instructorName;
    private LocalDateTime createdAt;
    private String handledByName;
    private String adminComments;
}