package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AcademicSemesterRequestDTO {

    @NotBlank(message = "Name is required")
    @Size(max = 255)
    private String name; 

    @NotNull(message = "Year is required")
    private Integer year; 

    @Size(max = 50)
    private String type; 
   
    private LocalDate startDate;

    private LocalDate endDate;
} 