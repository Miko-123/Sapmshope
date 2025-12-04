package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignStaffRequestDTO {

    @NotNull(message = "User ID is required to assign staff")
    private Integer userId;
}