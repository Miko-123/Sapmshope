package com.hopesapms.app.modules.attendance.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hopesapms.app.modules.user.model.User;

@Entity
@Table(name = "excused_absence_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExcusedAbsenceRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "department_id", nullable = false)
    private Integer departmentId;  

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @Column(name = "missed_period", length = 50, nullable = false)
    private String missedPeriod;

    @Column(name = "missed_date", nullable = false)
    private LocalDate missedDate;

    @Column(name = "makeup_date", nullable = false)
    private LocalDate makeupDate;

    @Column(name = "makeup_period", length = 50, nullable = false)
    private String makeupPeriod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hod_id", nullable = false)
    private User hod;  

    @Column(name = "hod_approval_date", nullable = false)
    private LocalDate hodApprovalDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "apo_id", nullable = false)
    private User apo;  

    @Column(name = "apo_approval_date", nullable = false)
    private LocalDate apoApprovalDate;

    @Column(length = 20, nullable = false)
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