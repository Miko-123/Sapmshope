package com.hopesapms.app.modules.academicsemester.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AcademicSemesterResponseDTO {
    private Long id;
    private String name;
    private Integer year;
    private String type;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isCurrent;
    private String status;
    private LocalDateTime createdAt;
}