package com.hopesapms.app.dto;

import lombok.Data;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class GradingScaleDTO {
    
    private Integer id;

    @NotBlank(message = "Letter grade is required")
    private String letterGrade;

    @NotNull(message = "Min score is required")
    @Min(0)
    @Max(100)
    private Double minScore;

    @NotNull(message = "Max score is required")
    @Min(0)
    @Max(100)
    private Double maxScore;

    @NotNull(message = "Grade point is required")
    private Double gradePoint;

    private String description;
}