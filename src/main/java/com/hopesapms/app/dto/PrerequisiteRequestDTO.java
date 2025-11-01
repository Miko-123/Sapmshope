package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrerequisiteRequestDTO {

    @NotNull(message = "The main course ID is required")
    private Integer courseId; // The course that *has* the prerequisite

    @NotNull(message = "The prerequisite course ID is required")
    private Integer prerequisiteCourseId; // The course that *is* the prerequisite

    private String minGrade;

    private Boolean isRequired;
}