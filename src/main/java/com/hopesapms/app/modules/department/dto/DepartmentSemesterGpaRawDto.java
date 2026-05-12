package com.hopesapms.app.modules.department.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentSemesterGpaRawDto {
    private String semester;
    private String department;
    private Double gpa;
    private Long enrolledStudents;

}
