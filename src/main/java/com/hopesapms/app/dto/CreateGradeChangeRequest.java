package com.hopesapms.app.dto;

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