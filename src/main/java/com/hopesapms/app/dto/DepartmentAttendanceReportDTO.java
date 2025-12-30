package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class DepartmentAttendanceReportDTO {
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstructorStats {
        private Long instructorId;
        private String instructorName;
        private Long totalSessionsScheduled;
        private Long presentCount;
        private Long absentCount;
        private Double attendanceRate; 
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentStats {
        private String studentId;
        private String studentName;
        private String courseCode;
        private String courseTitle;
        private String sectionName;
        
        private Long totalSessions;
        private Long absentSessions;
        
        private Double absencePercentage;
        
        private String status; 
    }
}
