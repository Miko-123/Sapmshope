package com.hopesapms.app.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssessmentRequestDTO {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotBlank(message = "Assessment name is required")
    private String name; 

    @NotBlank(message = "Assessment type is required")
    private String type; 

    @NotNull(message = "Max score is required")
    @Positive(message = "Max score must be positive")
    private BigDecimal maxScore; 

    @NotNull(message = "Weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be at least 0.01 (1%)")
    @DecimalMax(value = "1.00", message = "Weight cannot be more than 1.00 (100%)")
    private BigDecimal weight; 

    private LocalDateTime dueDate;
    private String description;
    
    
    private Boolean isBase; 
}