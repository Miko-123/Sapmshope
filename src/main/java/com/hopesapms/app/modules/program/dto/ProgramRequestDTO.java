package com.hopesapms.app.modules.program.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProgramRequestDTO {

    @NotBlank(message = "Program name is required")
    @Size(min = 3, max = 255)
    private String name;

    @NotBlank(message = "Program code is required")
    @Size(min = 2, max = 50)
    private String code;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Total credits required is required")
    @Positive(message = "Total credits must be a positive number")
    private Integer totalCreditRequired;

    private String description;
}