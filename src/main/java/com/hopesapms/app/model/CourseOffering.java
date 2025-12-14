package com.hopesapms.app.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.hopesapms.app.util.IntegerListConverter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Entity
@Table(name = "course_offering")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseOffering {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @OneToMany(mappedBy = "courseOffering", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<Assessment> assessments = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_semester_id", nullable = false)
    private AcademicSemester academicSemester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = true)
    private Instructor instructor;

    @Convert(converter = IntegerListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<Integer> yearLevels;

    @Column(name = "contact_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer contactHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(length = 50)
    private String status;

    @OneToMany(mappedBy = "courseOffering", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private Set<CourseSchedule> scheduleSlots = new HashSet<>();

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void addScheduleSlot(String day, String periods, String room) {
        CourseSchedule slot = CourseSchedule.builder()
                .courseOffering(this)
                .day(day)
                .periods(periods)
                .room(room)
                .build();
        this.scheduleSlots.add(slot);
    }
}