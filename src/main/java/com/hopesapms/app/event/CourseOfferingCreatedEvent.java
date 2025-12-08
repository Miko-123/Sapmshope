package com.hopesapms.app.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CourseOfferingCreatedEvent extends ApplicationEvent {
    private final Long courseOfferingId; 

    public CourseOfferingCreatedEvent(Object source, Long courseOfferingId) {
        super(source);
        this.courseOfferingId = courseOfferingId;
    }
}