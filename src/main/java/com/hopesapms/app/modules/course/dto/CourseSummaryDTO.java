package com.hopesapms.app.modules.course.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CourseSummaryDTO {
    private Integer id;
    private String title;
    private String courseCode;
}