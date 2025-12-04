package com.hopesapms.app.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StudentResponse {
    private Integer id;
    private Integer userId;
    private String studentId;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String phoneNumber;
    private String profilePictureUrl;
    private Long programId;
    private String programName;
    private Long departmentId;
    private String departmentName;
    private Integer sectionId;
    private String sectionName;
    private Integer yearLevel;
    private LocalDate enrollmentDate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}