package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAssessmentsModalityDTO {

    @NotBlank(message = "Component name is required")
    private String componentName;

    @NotBlank(message = "Component weight is required")
    private String componentWeight;

    @NotBlank(message = "Coverage is required")
    private String Coverage;
    
    @NotBlank(message = "Assessment time is required")
    private String assessmentTime;

    private String description;
}