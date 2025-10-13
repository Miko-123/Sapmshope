package com.hopesapms.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username is required") 
    @Size(min = 3, max = 100, message = "Username must ne 3-100 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotBlank(message = "Email is required") 
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank 
    private String middleName;

    @NotBlank(message = "Last name is required") 
    private String lastName;

    @NotBlank(message = "Gender is required") 
    private String gender;

    private String phoneNumber;
    private String profilePictureUrl;
    @NotNull(message = "Roles are required")
    private Set<Integer> roles;
}
