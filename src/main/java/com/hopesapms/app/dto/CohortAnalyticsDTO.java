package com.hopesapms.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortAnalyticsDTO {
    private String id;        // Program ID
    private String name;      // Program Name
    private long total;       // Total Students
    private long onTrack;     // Count of students on track
    private double completionRate; // Average completion rate
    private double avgGpa;    // Average GPA
}