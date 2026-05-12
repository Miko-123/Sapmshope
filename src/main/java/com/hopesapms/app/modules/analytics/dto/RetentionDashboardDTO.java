package com.hopesapms.app.modules.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.hopesapms.app.modules.department.dto.DepartmentRetentionDTO;

@Data
@Builder
public class RetentionDashboardDTO {
    private RetentionMetricDTO metrics;
    private List<RetentionTrendDTO> yearlyRetention;
    private List<ClassStandingRetentionDTO> retentionByClass;
    private List<DepartmentRetentionDTO> departmentAnalysis;
}