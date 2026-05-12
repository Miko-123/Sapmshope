package com.hopesapms.app.modules.department.dto;

public record DepartmentStatusDTO(
    String departmentName,
    long onTrack,
    long atRisk,
    long behind
) {}
