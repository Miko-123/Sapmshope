package com.hopesapms.app.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DepartmentResponseDTO {
    private Long id;
    private String name;
    private String code;
    private String contactEmail;
    private String contactPhone;
    private String officeLocation;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer departmentHeadId;
    private String departmentHeadName;
}