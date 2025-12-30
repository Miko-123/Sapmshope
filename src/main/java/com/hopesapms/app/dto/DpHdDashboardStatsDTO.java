package com.hopesapms.app.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class DpHdDashboardStatsDTO {

    private long totalStudents;
    private long totalInstructors;
    private long activeCourses;
    private double avgDepartmentGpa;

    private Map<String, Long> enrollmentByYear; 
    private Map<String, Long> courseStatusDistribution; 

    private List<AuditLogResponse> recentActivities;
    
}
