package com.hopesapms.app.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RegistrarDashboardDTO {
    
    private Long currentSemesterId;
    private String currentSemesterName;
    private String semesterStatus;
    
    private long totalActiveOfferings; 
    private long totalEnrolledStudents; 
    private long studentsWithPendingGrades;
         
    private double semesterProgressPercentage; 
}