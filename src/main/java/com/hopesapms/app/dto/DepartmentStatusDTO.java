package com.hopesapms.app.dto;

public record DepartmentStatusDTO(
    String departmentName,
    long onTrack,
    long atRisk,
    long behind
) {}
