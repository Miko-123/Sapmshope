package com.hopesapms.app.modules.student.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateStudentRequest {
    private String firstName;
    private String middleName;
    private String lastName;

    @Email
    private String email;

    private Long programId;
    private Long departmentId;
    private Long sectionId;

    private Integer yearLevel;
    private LocalDate enrollmentDate;
    private String status;
}