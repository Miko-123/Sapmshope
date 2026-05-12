package com.hopesapms.app.modules.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Set;

@Data
public class UpdateUserPrivilegesRequest {

    @NotEmpty(message = "At least one role ID must be provided")
    private Set<Integer> roleIds; 
}