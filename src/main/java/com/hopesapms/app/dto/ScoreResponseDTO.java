package com.hopesapms.app.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ScoreResponseDTO {
    private Integer id;
    private Integer enrollmentId;
    private Integer assessmentId;
    private BigDecimal scoreValue;
    private String recordedByUsername;
    private LocalDateTime recordedDate;
    private boolean isOverriden;
    private BigDecimal originalScoreValue;
}