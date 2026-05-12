package com.hopesapms.app.modules.course.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_objective")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseObjective {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String objective;
}