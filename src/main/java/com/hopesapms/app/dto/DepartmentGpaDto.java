package com.hopesapms.app.dto;

public record DepartmentGpaDto(
    Long departmentId,
    String departmentName,
    Double averageGpa
) {}

