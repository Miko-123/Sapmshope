package com.hopesapms.app.modules.academicsemester.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "academic_semester")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AcademicSemester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer year;

    @Column(length = 50)
    private String type;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean isCurrent = false;

    @Column(length = 20)
    private String status;

    public boolean isArchived() {
        return "ARCHIVED".equalsIgnoreCase(this.status);
    }

    public boolean isEditable() {
        return "ACTIVE".equalsIgnoreCase(this.status) || "PLANNED".equalsIgnoreCase(this.status);
    }

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Override
    public String toString() {
        return "AcademicSemester{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", year=" + year +
                ", type='" + type + '\'' +
                ", isCurrent=" + isCurrent +
                '}';
    }
}