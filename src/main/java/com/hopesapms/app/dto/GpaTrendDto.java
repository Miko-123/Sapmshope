package com.hopesapms.app.dto;

import java.util.Map;

import lombok.Data;

@Data
public class GpaTrendDto {
    private String semester;
    private Map<String, Double> departmentGpas;

    public GpaTrendDto(String semester, Map<String, Double> departmentGpas) {
        this.semester = semester;
        this.departmentGpas = departmentGpas;
    }
}
