package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AssessmentSummaryDTO {
    private String name;
    private BigDecimal weight;
    private BigDecimal maxScore;
}