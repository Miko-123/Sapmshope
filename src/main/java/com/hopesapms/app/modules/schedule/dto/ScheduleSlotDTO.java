package com.hopesapms.app.modules.schedule.dto;

import lombok.Data;

@Data
public class ScheduleSlotDTO {

    private String day; // "Mon", "Thr"

    private String periods; // "3,4", "1,2"

    private String room; // "L-301"
}