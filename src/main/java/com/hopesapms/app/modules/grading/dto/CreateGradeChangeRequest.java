package com.hopesapms.app.modules.grading.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateGradeChangeRequest {
    @NotNull
    private Long enrollmentId;
    
    @NotNull
    private Integer assessmentId;
    
    @NotNull
    private Double newScore;
    
    @NotNull
    private String reason;
}