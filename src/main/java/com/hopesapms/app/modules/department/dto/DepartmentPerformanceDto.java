package com.hopesapms.app.modules.department.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentPerformanceDto {
    private String department;
    private String semester;
    private Double currentGpa;
    private Long enrolledStudents;
    private Double semesterChange;
    private String status;
}
