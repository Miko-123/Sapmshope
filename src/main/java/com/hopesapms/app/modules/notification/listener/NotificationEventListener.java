package com.hopesapms.app.modules.notification.listener;

import com.hopesapms.app.common.event.AppEvents;
import com.hopesapms.app.modules.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @EventListener
    @Async
    public void handleGradePosted(AppEvents.GradePostedEvent event) {
        String title = "New Grade Posted";
        String message = String.format("You received a score of %.1f on %s in %s.", 
            event.getScore(), event.getAssessmentName(), event.getCourseName());
        
        notificationService.createNotification(
            event.getStudent().getId(), 
            title, 
            message, 
            "ACADEMIC"
        );
    }

    @EventListener
    @Async
    public void handleLowAttendance(AppEvents.LowAttendanceEvent event) {
        String title = "Low Attendance Warning";
        String message = String.format("Warning: Your attendance in %s has dropped to %.1f%%. Please contact your instructor.", 
            event.getCourseName(), event.getPercentage());
        
        notificationService.createNotification(
            event.getStudent().getId(), 
            title, 
            message, 
            "AT_RISK"
        );
    }

    @EventListener
    @Async
    public void handleCourseAssigned(AppEvents.CourseAssignedEvent event) {
        String title = "New Course Assignment";
        String message = String.format("You have been assigned to teach %s - Section %s.", 
            event.getCourseCode(), event.getSectionName());
        
        notificationService.createNotification(
            event.getInstructor().getId(), 
            title, 
            message, 
            "ADMIN"
        );
    }
}
