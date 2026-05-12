package com.hopesapms.app.modules.score.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class StudentScoreDTO {

    private String assessmentName; 
    private String assessmentType; 
    private BigDecimal maxScore;   

    private Integer scoreId;
    private BigDecimal scoreValue; 
    private LocalDateTime recordedDate;
    private String recordedBy; 
}