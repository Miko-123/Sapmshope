package com.hopesapms.app.modules.user.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

import com.hopesapms.app.modules.auditlog.dto.AuditLogResponse;

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
