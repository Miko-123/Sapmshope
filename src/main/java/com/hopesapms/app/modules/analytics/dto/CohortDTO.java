package com.hopesapms.app.modules.analytics.dto;

public record CohortDTO(
    String id,
    String name,
    long total,
    long onTrack,
    double completionRate,
    double avgGpa
) {}
