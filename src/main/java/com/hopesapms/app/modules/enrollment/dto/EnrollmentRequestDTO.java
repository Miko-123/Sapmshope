package com.hopesapms.app.modules.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequestDTO {
    
    @NotNull(message = "Student ID is required")
    private Integer studentId;

    @NotNull(message = "Course Offering ID is required")
    private Long courseOfferingId;
}