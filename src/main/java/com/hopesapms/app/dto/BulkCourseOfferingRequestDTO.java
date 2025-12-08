package com.hopesapms.app.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkCourseOfferingRequestDTO {
   
    private Integer courseId;
    private Long academicSemesterId;
    private Integer contactHours;
    private List<Integer> yearLevels;
    private String status; 

    private List<SectionInstructorPair> assignments;

    @Data
    public static class SectionInstructorPair {
        private Integer sectionId;
        private Long instructorId;
    }
}