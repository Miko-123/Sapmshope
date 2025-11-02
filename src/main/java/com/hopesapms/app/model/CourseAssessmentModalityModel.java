package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_assessment_modality")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CourseAssessmentModalityModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instructor_id", nullable = false, unique = true)
    private Instructor instructorId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false, unique = true)
    private Course courseId;

    @Column(name = "component_name", length = 255, nullable = false, unique = true)
    private String componentName;

    @Column(name = "component_weight",  length = 50, nullable = false)
    private String componentWeight;

    @Column(name="assessment_time", length = 100, nullable = false)
    private String assessmentTime;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_base", nullable = false)
    @Builder.Default
    private boolean isBase = false;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}