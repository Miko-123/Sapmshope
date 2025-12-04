package com.hopesapms.app.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateSectionRequestDTO {
    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    @Min(1)
    private Integer yearLevel;

    @NotNull
    @Min(1)
    private Integer capacity;

    @NotNull
    private Long programId;
}