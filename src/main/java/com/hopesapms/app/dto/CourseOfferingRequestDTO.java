package com.hopesapms.app.dto;

import jakarta.validation.Valid;
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
    private Integer sectionId;

    @NotBlank
    private String status; // "PLANNED", "ACTIVE"

    @NotNull
    @Size(min = 1, message = "At least one schedule slot is required")
    @Valid 
    private List<ScheduleSlotDTO> scheduleSlots;
}