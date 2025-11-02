package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "students", indexes = {
    @Index(columnList = "user_id"),
    @Index(columnList = "student_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="course_id", nullable = false)
    private Course courseId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="instructor_id", nullable = false)
    private Instructor instructorId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="semester_id", nullable = false)
    private Program programId;

    
}
