package com.hopesapms.app.modules.academicsemester.dto;

public record StatusSummaryDTO(
    long onTrack,
    long atRisk,
    long behindSchedule,
    long total
) {}