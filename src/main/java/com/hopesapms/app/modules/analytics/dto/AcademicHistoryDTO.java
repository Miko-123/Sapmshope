package com.hopesapms.app.modules.analytics.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

import com.hopesapms.app.modules.student.dto.StudentCourseDTO;

@Data
@Builder
public class AcademicHistoryDTO {
    private String studentName;
    private String studentId;
    private Double cumulativeGPA;
    private Integer totalCreditsEarned;
    private List<SemesterRecordDTO> semesters;

    @Data
    @Builder
    public static class SemesterRecordDTO {
        private Long semesterId;
        private String semesterName;
        private Integer year;
        private String status; 
        private Double semesterGPA;
        private Integer semesterCredits;
        private List<StudentCourseDTO> courses;
    }
}