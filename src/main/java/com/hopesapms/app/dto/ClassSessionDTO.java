package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassSessionDTO {
    private Integer id;
    private Integer courseId;
    private LocalDate sessionDate;
    private LocalTime sessionTime;
    private String location;
    private Integer scheduledInstructorId;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}