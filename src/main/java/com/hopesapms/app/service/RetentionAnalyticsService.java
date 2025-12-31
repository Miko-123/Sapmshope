// File: src/main/java/com/hopesapms/app/service/RetentionAnalyticsService.java
package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RetentionAnalyticsService {

    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public RetentionDashboardDTO getDashboardData() {
        // 1. Calculate Summary Metrics
        long totalStudents = studentRepository.countTotalStudents();
        long activeStudents = studentRepository.countActiveStudents();
        long attritionCount = studentRepository.countAttritionStudents();
        
        // Avoid division by zero
        double retentionRate = totalStudents == 0 ? 0 : ((double) activeStudents / totalStudents) * 100;
        double attritionRate = totalStudents == 0 ? 0 : ((double) attritionCount / totalStudents) * 100;

        RetentionMetricDTO metrics = RetentionMetricDTO.builder()
                .overallRetentionRate(Math.round(retentionRate * 10.0) / 10.0)
                .totalEnrolled(activeStudents)
                .attritionRate(Math.round(attritionRate * 10.0) / 10.0)
                .targetRate(90.0) // Hardcoded institutional target
                .build();

        // 2. Yearly Trends (Historical)
        List<Object[]> cohortData = studentRepository.findRetentionByYearCohort();
        List<RetentionTrendDTO> trends = cohortData.stream().map(obj -> {
            int year = (Integer) obj[0];
            long total = (Long) obj[1];
            long retained = (Long) obj[2]; // Sum returned as Long
            double rate = total == 0 ? 0 : ((double) retained / total) * 100;
            return new RetentionTrendDTO(String.valueOf(year), Math.round(rate * 10.0) / 10.0, 90.0);
        }).collect(Collectors.toList());

        // 3. Class Standing (Retention by Year Level)
        // Note: Real retention by class standing requires historical snapshots. 
        // For this MVP, we map current active counts to a visual representation relative to a base 100% capacity
        // or we simply return the distribution as "Retention" proxies.
        List<Object[]> levelData = studentRepository.findActiveCountByYearLevel();
        double freshmen = 0, soph = 0, junior = 0, senior = 0;
        
        for (Object[] row : levelData) {
            int year = (Integer) row[0];
            long count = (Long) row[1];
            // Normalize slightly for the chart (assuming batch size of ~100 for visualization)
            // Or ideally, query actual retention per year level. Here we map Counts.
            double val = count; 
            switch (year) {
                case 1 -> freshmen = val;
                case 2 -> soph = val;
                case 3 -> junior = val;
                default -> senior += val; // 4 and 5
            }
        }
        
        // Mocking semester snapshots for the bar chart comparison
        List<ClassStandingRetentionDTO> standing = new ArrayList<>();
        standing.add(new ClassStandingRetentionDTO("Current Sem", freshmen, soph, junior, senior));

        // 4. Department Analysis
        List<DepartmentRetentionDTO> departments = studentRepository.getRawDepartmentRetentionStats();
        departments.forEach(d -> {
            // Recalculate Rate and Risk based on raw sums
            long total = d.getEnrolled(); // In the query, 'enrolled' held the total count
            long attrition = d.getAttrition();
            long active = total - attrition;
            
            double rate = total == 0 ? 0 : ((double) active / total) * 100;
            d.setRetention(Math.round(rate * 10.0) / 10.0);
            
            // Set Enrolled to active count for display
            d.setEnrolled(active);

            if (rate < 75) d.setRiskLevel("High");
            else if (rate < 85) d.setRiskLevel("Medium");
            else d.setRiskLevel("Low");
        });

        return RetentionDashboardDTO.builder()
                .metrics(metrics)
                .yearlyRetention(trends)
                .retentionByClass(standing)
                .departmentAnalysis(departments)
                .build();
    }
}