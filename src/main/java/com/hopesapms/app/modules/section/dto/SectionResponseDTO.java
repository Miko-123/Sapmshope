package com.hopesapms.app.modules.section.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SectionResponseDTO {
    private Integer id;
    private String name;
    private Integer yearLevel;
    private Integer capacity;
    private Long programId;
    private String programName;
    private String departmentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}