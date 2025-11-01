package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseRequestDTO {

    @NotBlank(message = "Course title is required")
    @Size(max = 255)
    private String title;

    @NotBlank(message = "Course code is required")
    @Size(max = 50)
    private String courseCode;

    @NotNull(message = "Credits are required")
    @Positive(message = "Credits must be a positive number")
    private Double credits;

    @NotNull(message = "Program ID is required")
    private Long programId;

    private String description;
    
}