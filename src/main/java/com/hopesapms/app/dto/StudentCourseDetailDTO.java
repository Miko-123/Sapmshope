package com.hopesapms.app.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class StudentCourseDetailDTO {
    private Long enrollmentId;
    private String courseTitle;
    private String courseCode;
    private String instructorName;
    
    private Double totalGrade;       // e.g. 85.0
    private Double maxPossibleGrade;
    private String letterGrade; 
    private Double gradePoint; 
    
    private List<StudentAssessmentDTO> assessments = new ArrayList<>();
    private List<StudentAttendanceDTO> attendance = new ArrayList<>();
    
    @Data
    public static class StudentAssessmentDTO {
        private String assessmentName;
        private String type;
        private Double weight;
        private Double maxScore;
        private Double scored;
        private String status; // "GRADED", "PENDING", "UPCOMING"
    }

    @Data
    public static class StudentAttendanceDTO {
        private String date;
        private String status; // "PRESENT", "ABSENT"
        private String remarks;
    }
}