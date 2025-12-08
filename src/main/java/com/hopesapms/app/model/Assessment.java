package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

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

    // Change to instructor id instead of user id for easier readability maybe...(do research later)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private Instructor instructorId;

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

    @OneToMany(mappedBy = "assessment", fetch = FetchType.LAZY)
    private Set<Score> scores;
}