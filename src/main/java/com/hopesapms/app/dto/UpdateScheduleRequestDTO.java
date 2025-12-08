package com.hopesapms.app.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateScheduleRequestDTO {

    @NotNull(message = "Status is required")
    private String status;

    @Valid
    private List<ScheduleSlotDTO> scheduleSlots; 
    
}
