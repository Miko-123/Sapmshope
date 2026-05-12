package com.hopesapms.app.modules.score.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class BulkScoreRequestDTO {

    @NotNull(message = "Assessment ID is required")
    private Integer assessmentId;

    @Valid
    private List<StudentScoreEntry> scores;

    @Data
    public static class StudentScoreEntry {
        @NotNull(message = "Enrollment ID is required")
        private Integer enrollmentId;

        @NotNull(message = "Score value is required")
        @PositiveOrZero(message = "Score must be a positive number or zero")
        private BigDecimal scoreValue;
    }
}