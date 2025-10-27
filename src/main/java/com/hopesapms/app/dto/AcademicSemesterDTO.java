package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AcademicSemesterDTO {

    private Long id;

    @NotBlank(message = "Semester name is required (e.g., Semester 1 2024/25)")
    private String name;

    @NotNull(message = "Year is required (e.g., 2024)")
    private Integer year;

    @NotBlank(message = "Type must be 'Semester 1' or 'Semester 2'")
    @Pattern(regexp = "^(Semester 1|Semester 2)$", message = "Type must be 'Semester 1' or 'Semester 2'")
    private String type;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private Boolean isCurrent;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}