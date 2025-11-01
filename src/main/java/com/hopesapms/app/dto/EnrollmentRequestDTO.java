package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EnrollmentRequestDTO {

    @NotNull(message = "Course ID is required to enroll")
    private Integer courseId;
}