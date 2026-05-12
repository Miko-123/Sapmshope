package com.hopesapms.app.common.event;

import com.hopesapms.app.modules.user.model.User;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

public class AppEvents {
    @Getter
    public static class GradePostedEvent extends ApplicationEvent {
        private final User student;
        private final String courseName;
        private final String assessmentName;
        private final Double score;

        public GradePostedEvent(Object source, User student, String courseName, String assessmentName, Double score) {
            super(source);
            this.student = student;
            this.courseName = courseName;
            this.assessmentName = assessmentName;
            this.score = score;
        }
    }

    @Getter
    public static class LowAttendanceEvent extends ApplicationEvent {
        private final User student;
        private final String courseName;
        private final double percentage;

        public LowAttendanceEvent(Object source, User student, String courseName, double percentage) {
            super(source);
            this.student = student;
            this.courseName = courseName;
            this.percentage = percentage;
        }
    }

    @Getter
    public static class CourseAssignedEvent extends ApplicationEvent {
        private final User instructor;
        private final String courseCode;
        private final String sectionName;

        public CourseAssignedEvent(Object source, User instructor, String courseCode, String sectionName) {
            super(source);
            this.instructor = instructor;
            this.courseCode = courseCode;
            this.sectionName = sectionName;
        }
    }
}
