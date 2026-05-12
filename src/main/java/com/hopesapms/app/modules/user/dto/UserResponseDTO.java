package com.hopesapms.app.modules.user.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
public class UserResponseDTO {
    private Integer id;
    private String studentId;
    private String username;
    private String email;
    private String firstName;
    private String middleName;
    private String lastName;
    private String gender;
    private String phoneNumber;
    private String profilePictureUrl;
    private Integer programId;
    private Integer departmentId;
    private Integer yearLevel;
    private LocalDate enrollmentDate;
    private String status;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<RoleResponse> roles;
    private String errorMessage;

    @Data
    public static class RoleResponse {
        private Integer id;
        private String name;
    }
}