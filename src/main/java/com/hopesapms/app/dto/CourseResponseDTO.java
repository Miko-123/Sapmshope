package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class CourseResponseDTO {

    private Integer id;
    private String title;
    private String courseCode;
    private Double credits;
    private String description;

    private Integer yearLevel;
    private List<PrerequisiteResponseDTO> prerequisites;
    private String category;
    private Long programId;
    private String programName;
    private Long departmentId;
    private String departmentName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}