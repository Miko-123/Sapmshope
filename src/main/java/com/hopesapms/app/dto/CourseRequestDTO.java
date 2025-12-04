package com.hopesapms.app.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CourseRequestDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String courseCode;

    @NotNull
    @Positive
    private Double credits;

    @NotNull
    private Long programId;

    private Integer yearLevel;

    @NotBlank
    private String category;

    private List<PrerequisiteRequestDTO> prerequisites;

    private String description;
}
