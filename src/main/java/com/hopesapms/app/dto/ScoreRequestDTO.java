package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ScoreRequestDTO {

    @NotNull(message = "Enrollment ID is required")
    private Integer enrollmentId;

    @NotNull(message = "Assessment ID is required")
    private Integer assessmentId;

    @NotNull(message = "Score value is required")
    @PositiveOrZero(message = "Score must be a positive number or zero")
    private BigDecimal scoreValue;
}