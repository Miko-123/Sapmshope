package com.hopesapms.app.modules.instructor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstructorAttendanceDashboardDTO {
    private AttendanceMetricsDTO metrics;
    private List<MonthlyTrendDTO> monthlyTrend;
    private List<DeptAttendanceDTO> departmentAttendance;
    private List<AbsenceReasonDTO> absenceReasons;
    private List<DeptSummaryDTO> departmentSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendanceMetricsDTO {
        private String averageAttendance; // e.g., "96.4%"
        private long totalFaculty;
        private long perfectAttendance;
        private long belowTarget;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrendDTO {
        private String month; // e.g., "Aug", "Sep"
        private double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptAttendanceDTO {
        private String department;
        private double rate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbsenceReasonDTO {
        private String reason;
        private long count;
        private double percentage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeptSummaryDTO {
        private String id; // Department ID or Name
        private String department;
        private long faculty;
        private double avgAttendance;
        private long perfectRecord;
        private long belowTarget;
        private String status; // "Excellent", "Good", "Needs Review"
    }
}