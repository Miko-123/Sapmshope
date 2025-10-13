package com.hopesapms.app.dto;

import lombok.Data;
import jakarta.validation.constraints.*;
import java.util.Set; 

@Data
public class UpdateUserRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Middle name is required")
    private String middleName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Gender is required")
    private String gender;

    private String phoneNumber;
    private String profilePictureUrl;
    
    @NotNull(message = "Roles are required")
    private Set<Integer> roleIds;
}
