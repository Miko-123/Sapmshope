package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeptCourseDTO {
    private Long offeringId;
    private String courseName;
    private String courseCode;
    private String sectionName;
    private String instructorName;
    private String semesterName;
    private Long studentCount;
}