package com.hopesapms.app.modules.room.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // e.g., "B-201", "Lab-3"

    private Integer capacity; 

    private String type; // "LECTURE_HALL", "LABORATORY", "CLASSROOM"
    
    private boolean isDeleted = false;
}