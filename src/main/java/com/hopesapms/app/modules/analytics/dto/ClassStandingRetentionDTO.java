package com.hopesapms.app.modules.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ClassStandingRetentionDTO {
    private String semester;
    private double freshmen;
    private double sophomore;
    private double junior;
    private double senior;
}