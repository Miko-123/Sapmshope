package com.hopesapms.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.List;

@Data
public class CourseOfferingRequestDTO {

    @NotNull
    private Integer courseId;

    @NotNull
    private Long academicSemesterId;

    @NotNull
    private Integer instructorId;

    @NotNull
    @Size(min = 1, message = "At least one year level must be selected")
    private List<Integer> yearLevels;

    @NotNull(message = "Contact hours are required")
    @Min(value = 1, message = "Contact hours must be at least 1")
    private Integer contactHours;

    @NotNull
    private Integer sectionId;

    @NotBlank
    private String status;

    @Valid
    private List<ScheduleSlotDTO> scheduleSlots;
}