package com.hopesapms.app.modules.department.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentGraduationStatusDTO {
    private String departmentName;
    private long onTrack;
    private long atRisk;
    private long behind;
}