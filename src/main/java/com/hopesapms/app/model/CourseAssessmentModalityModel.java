package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_assessment_modality")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CourseAssessmentModalityModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}