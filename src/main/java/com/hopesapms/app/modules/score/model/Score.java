package com.hopesapms.app.modules.score.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hopesapms.app.modules.assessment.model.Assessment;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.user.model.User;

@Entity
@Table(name = "score")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Score {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private Enrollment enrollment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "score_value", precision = 5, scale = 2, nullable = false)
    private BigDecimal scoreValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @Column(name = "recorded_date", nullable = false)
    private LocalDateTime recordedDate;

    @Column(name = "is_overriden", nullable = false)
    @Builder.Default
    private boolean isOverriden = false;

    @Column(name = "original_score_value", precision = 5, scale = 2)
    private BigDecimal originalScoreValue;

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