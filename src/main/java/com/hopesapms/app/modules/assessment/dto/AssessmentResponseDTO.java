package com.hopesapms.app.modules.assessment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssessmentResponseDTO {
    private Integer id;
    private Integer courseId;
    private Long courseOfferingId;
    private String name;
    private String type;
    private BigDecimal maxScore;
    private BigDecimal weight;
    private LocalDateTime dueDate;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private boolean isBase;
}