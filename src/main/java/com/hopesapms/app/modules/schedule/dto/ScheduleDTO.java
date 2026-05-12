package com.hopesapms.app.modules.schedule.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ScheduleDTO {
    private Integer classSessionId;
    private String courseCode;
    private String courseTitle;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String location;
    private String instructorName;
}