package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RetentionTrendDTO {
    private String year;
    private double rate;
    private double target;
}