package com.hopesapms.app.dto;

public record CohortDTO(
    String id,
    String name,
    long total,
    long onTrack,
    double completionRate,
    double avgGpa
) {}
