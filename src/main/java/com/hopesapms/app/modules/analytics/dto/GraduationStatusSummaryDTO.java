package com.hopesapms.app.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraduationStatusSummaryDTO {
    private long onTrack;
    private long atRisk;
    private long behindSchedule;
    private long total;
}