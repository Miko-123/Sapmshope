package com.hopesapms.app.modules.courseoffering.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignStaffRequestDTO {

    @NotNull(message = "User ID is required to assign staff")
    private Integer userId;
}