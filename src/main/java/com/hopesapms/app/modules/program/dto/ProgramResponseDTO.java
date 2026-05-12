package com.hopesapms.app.modules.program.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ProgramResponseDTO {
    private Long id;
    private String name;
    private String code;
    private Integer totalCreditRequired;
    private String description;
    
    // Nested info for a better UI
    private Long departmentId;
    private String departmentName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}