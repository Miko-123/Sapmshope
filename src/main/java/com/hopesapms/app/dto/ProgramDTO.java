package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProgramDTO {
    private Long id;

    @NotBlank(message = "Program name is required")
    @Size(min = 3, max = 255, message = "Name must be between 3 and 255 characters")
    private String name;

    @NotBlank(message = "Program code is required")
    @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
    private String code;

    @NotNull(message = "Department ID is required")
    private Long departmentId;

    @NotNull(message = "Total credits required is required")
    private Integer totalCreditRequired;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
