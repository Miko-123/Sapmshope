package com.hopesapms.app.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import com.hopesapms.app.model.Role;
import com.hopesapms.app.model.User;

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

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.middleName = user.getMiddleName();
        this.lastName = user.getLastName();
        this.phoneNumber = user.getPhoneNumber();
        this.profilePictureUrl = user.getProfilePictureUrl();
        this.isDeleted = user.isDeleted();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();

        // Map roles if needed
        this.roles = user.getRoles()
                .stream()
                .map(RoleResponse::new) // Requires a RoleResponse(Role role) constructor
                .collect(Collectors.toSet());

        this.errorMessage = null; // or leave it as default
    }

    @Data
    public static class RoleResponse {
        private Integer id;
        private String name;

        public RoleResponse(Role role) {
            if (role == null)
                return;
            this.id = role.getId();
            this.name = role.getName();
        }
    }
}