package com.hopesapms.app.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterAcademicRequest {
    @NotBlank
    @Email
    private String email;
    @NotBlank
    private String studentId;
    @NotNull
    private Integer programId;
    @NotNull
    private Integer departmentId;
    @NotNull
    private Integer yearLevel;
    @NotNull
    private LocalDate enrollmentDate;
    @NotBlank
    private String status;
    @NotBlank
    private String firstName;
    private String middleName;
    @NotBlank
    private String lastName;
}