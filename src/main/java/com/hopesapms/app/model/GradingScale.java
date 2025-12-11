package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "grading_scales")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradingScale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 5)
    private String letterGrade; // A, A-, B+, etc.

    @Column(nullable = false)
    private Double minScore;    // e.g., 90.0

    @Column(nullable = false)
    private Double maxScore;    // e.g., 100.0

    @Column(nullable = false)
    private Double gradePoint;  // e.g., 4.0, 3.75

    private String description; // Excellent, Very Good, etc.
}