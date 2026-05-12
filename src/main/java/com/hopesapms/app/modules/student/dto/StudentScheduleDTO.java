package com.hopesapms.app.modules.student.dto;

import lombok.Data;

@Data
public class StudentScheduleDTO {
    private String day;        
    private String courseCode; 
    private String courseTitle;
    private String time;       
    private String room;       
    private String instructor; 
    private String color;      
}