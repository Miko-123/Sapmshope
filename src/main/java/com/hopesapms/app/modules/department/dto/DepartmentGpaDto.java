package com.hopesapms.app.modules.department.dto;

public record DepartmentGpaDto(
    Long departmentId,
    String departmentName,
    Double averageGpa
) {}

