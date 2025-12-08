package com.hopesapms.app.model;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "instructors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Instructor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(name = "office_location", length = 100)
    private String officeLocation;

    @Builder.Default
    @OneToMany(mappedBy = "instructor", fetch = FetchType.LAZY)
    private Set<CourseOffering> courseOfferings = new HashSet<>();
    
    @Builder.Default
    @OneToMany(mappedBy = "recordedBy", fetch = FetchType.LAZY)
    private Set<Score> scores = new HashSet<>();

}
