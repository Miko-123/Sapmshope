package com.hopesapms.app.event;

import com.hopesapms.app.model.Student;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class StudentRegisteredEvent extends ApplicationEvent {
    private final Student student;

    public StudentRegisteredEvent(Object source, Student student) {
        super(source);
        this.student = student;
    }
}