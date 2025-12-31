package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DepartmentRetentionDTO {
    private String department;
    private double retention;
    private long enrolled;
    private long attrition;
    private String riskLevel;
}