package com.hopesapms.app.modules.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RetentionMetricDTO {
    private double overallRetentionRate;
    private long totalEnrolled;
    private double attritionRate;
    private double targetRate;
}