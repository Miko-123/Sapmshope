package com.hopesapms.app.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class RetentionDashboardDTO {
    private RetentionMetricDTO metrics;
    private List<RetentionTrendDTO> yearlyRetention;
    private List<ClassStandingRetentionDTO> retentionByClass;
    private List<DepartmentRetentionDTO> departmentAnalysis;
}