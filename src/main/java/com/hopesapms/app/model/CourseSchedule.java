package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "course_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_offering_id", nullable = false)
    private CourseOffering courseOffering;

    @Column(length = 20, nullable = false)
    private String day; 

    @Column(length = 50, nullable = false)
    private String periods; 

    @Column(length = 50)
    private String room;
}