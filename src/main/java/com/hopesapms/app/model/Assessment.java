package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assessment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User userId;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(length = 50, nullable = false)
    private String type; 

    @Column(name = "max_score", precision = 5, scale = 2, nullable = false)
    private BigDecimal maxScore;

    @Column(precision = 5, scale = 2, nullable = false)
    private BigDecimal weight;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_base", nullable = false)
    @Builder.Default
    private boolean isBase = false;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String status;

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