package com.hopesapms.app.dto;

import lombok.Data;

@Data
public class PrerequisiteResponseDTO {
    private Integer id;
    private Integer prerequisiteCourseId;
    private String prerequisiteCourseCode;
    private String prerequisiteCourseTitle;
    private boolean isRequired;
}

