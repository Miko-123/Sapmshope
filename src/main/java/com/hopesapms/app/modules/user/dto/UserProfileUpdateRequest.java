package com.hopesapms.app.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserProfileUpdateRequest {
    
    @NotBlank(message = "Username is required")
    private String username;

    private String phoneNumber;
    
    private String gender; 

    private String profilePictureUrl;
}