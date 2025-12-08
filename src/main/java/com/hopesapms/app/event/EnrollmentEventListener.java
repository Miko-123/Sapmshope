package com.hopesapms.app.event;

import com.hopesapms.app.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase; 
import org.springframework.transaction.event.TransactionalEventListener; 

@Component
@RequiredArgsConstructor
public class EnrollmentEventListener {

    private final EnrollmentService enrollmentService;

   
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStudentRegistered(StudentRegisteredEvent event) {
        try {
            enrollmentService.enrollNewStudentInSection(event.getStudent());
        } catch (Exception e) {
            System.err.println("Error processing student registration event: " + e.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCourseOfferingCreated(CourseOfferingCreatedEvent event) {
        try {
            
            enrollmentService.enrollSectionInNewOffering(event.getCourseOfferingId());
        } catch (Exception e) {
            e.printStackTrace(); 
        }
    }
}