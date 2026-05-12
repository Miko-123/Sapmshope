package com.hopesapms.app.modules.academicsemester.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BatchPromoteRequest {
    @NotNull(message = "Program is required")
    private Integer programId;

    @NotNull(message = "Current Year Level is required")
    private Integer currentYearLevel;

    @NotNull(message = "Next Year Level is required")
    private Integer nextYearLevel; 
    
    private boolean isGraduating; 
}