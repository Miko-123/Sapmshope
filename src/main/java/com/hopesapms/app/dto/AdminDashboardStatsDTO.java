package com.hopesapms.app.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdminDashboardStatsDTO {
    private long totalUsers;
    private long totalStudents;
    private long totalInstructors;
    private long activeUsersNow; 
    private long recentSecurityAlerts; 
    
   
    private Map<String, Long> userRoleDistribution;
    private Map<String, Long> loginActivityLast7Days;
    
    private List<AuditLogResponse> recentAuditLogs;
}
