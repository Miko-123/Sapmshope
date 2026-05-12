package com.hopesapms.app.modules.score.dto;

import lombok.Data;

@Data
public class SaveScoreRequestDTO {
    private Long enrollmentId; 
    private Integer assessmentId;
    private Double score;
}