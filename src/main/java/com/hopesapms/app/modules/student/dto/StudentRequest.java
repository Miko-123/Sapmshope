package com.hopesapms.app.modules.student.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentRequest {
    @NotBlank(message = "Student Id is required")
    private String studentId;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Middle name is required")
    private String middleName;

    private String lastName;

    @Email(message = "Valid email is required")
    @NotBlank(message = "Email is required")
    private String email;

    @NotBlank(message = "Gender is required")
    private String gender;

    private String phoneNumber;

    @NotBlank
    private String department;

    @NotBlank
    private String program;

    @NotNull
    private Integer yearLevel;

    @NotNull(message = "Enrollment date is required")
    private LocalDate enrollmentDate;

    @NotBlank
    private String status;
}
