package com.hopesapms.app.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ScheduleSlotDTO {

    @NotBlank(message = "Day is required")
    private String day; // "Mon", "Thr"

    @NotBlank(message = "Periods are required")
    private String periods; // "3,4", "1,2"

    private String room; // "L-301"
}