package com.hopesapms.app.dto;

public record StatusSummaryDTO(
    long onTrack,
    long atRisk,
    long behindSchedule,
    long total
) {}