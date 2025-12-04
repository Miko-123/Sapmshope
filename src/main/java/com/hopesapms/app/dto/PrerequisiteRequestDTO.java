package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PrerequisiteRequestDTO {

    @NotNull(message = "Prerequisite course ID is required")
    private Integer prerequisiteCourseId;

    private Boolean isRequired = true;
}
