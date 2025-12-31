package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "grading_scale_detail")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradingScaleDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grading_scale_id", nullable = false)
    private GradingScale gradingScale;

    @Column(length = 10, nullable = false)
    private String grade;

    @Column(name = "min_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal minPercentage;

    @Column(name = "max_percentage", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxPercentage;
}