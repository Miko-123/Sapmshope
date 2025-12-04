package com.hopesapms.app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDepartmentDetailsRequest {

    @NotBlank(message = "Department code is required")
    @Size(min = 2, max = 50, message = "Code must be between 2 and 50 characters")
    private String code;

    @Email(message = "Invalid email format")
    private String contactEmail;
    
    private String contactPhone;
    private String officeLocation;
}