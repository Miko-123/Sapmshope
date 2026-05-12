package com.hopesapms.app.modules.student.dto;

import lombok.Data;

@Data
public class StudentCourseDTO {
    private Long enrollmentId;
    private Long courseOfferingId;
    private String courseCode;
    private String courseTitle;
    private String sectionName;
    private Integer contactHours;
    private String instructorName;
    private String semesterName;
    private String status; 
    private Double currentGrade; 
    private String letterGrade;
}