package com.hopesapms.app.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ClassSessionRequestDTO {

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotNull(message = "Session date is required")
    @FutureOrPresent(message = "Session date must be in the present or future")
    private LocalDate sessionDate;

    @NotNull(message = "Session time is required")
    private LocalTime sessionTime;

    @NotBlank(message = "Location is required")
    private String location;

    @NotNull(message = "Instructor ID is required")
    private Integer scheduledInstructorId;
}