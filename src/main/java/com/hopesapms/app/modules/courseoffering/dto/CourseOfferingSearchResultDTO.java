package com.hopesapms.app.modules.courseoffering.dto;

import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import com.hopesapms.app.modules.user.model.User;
import lombok.Data;

@Data
public class CourseOfferingSearchResultDTO {
    
    private Long id; 
    private String courseName;
    private String sectionName;
    private String instructorName;

    public CourseOfferingSearchResultDTO(CourseOffering offering) {
        this.id = offering.getId();
        this.courseName = offering.getCourse().getTitle();
        this.sectionName = offering.getSection().getName();
        
        if (offering.getInstructor() != null && offering.getInstructor().getUser() != null) {
            User instructorUser = offering.getInstructor().getUser();
            this.instructorName = instructorUser.getFirstName() + 
                                  " " + 
                                  instructorUser.getLastName();
        } else {
            this.instructorName = "Not Assigned";
        }
    }
}