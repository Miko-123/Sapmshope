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
    private String letterGrade; 

    @Column(nullable = false)
    private Double minScore;    

    @Column(nullable = false)
    private Double maxScore;    

    @Column(nullable = false)
    private Double gradePoint;  

    private String description;
}