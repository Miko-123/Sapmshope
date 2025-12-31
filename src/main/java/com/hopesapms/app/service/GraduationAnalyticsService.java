package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.EnrollmentRepository;
import com.hopesapms.app.repository.StudentRepository;
import com.hopesapms.app.util.GpaCalculationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GraduationAnalyticsService {

    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;

    // --- Threshold Configuration ---
    private static final double MIN_GPA_ON_TRACK = 2.0;
    private static final double MIN_COMPLETION_RATE_ON_TRACK = 85.0;
    private static final double MIN_COMPLETION_RATE_AT_RISK = 70.0;

    @Transactional(readOnly = true)
    public GraduationStatusSummaryDTO getStatusSummary(Long semesterId) {
        List<StudentAnalysis> analysisList = analyzeAllStudents();

        long onTrack = analysisList.stream().filter(a -> a.status == StudentStatus.ON_TRACK).count();
        long atRisk = analysisList.stream().filter(a -> a.status == StudentStatus.AT_RISK).count();
        long behind = analysisList.stream().filter(a -> a.status == StudentStatus.BEHIND).count();

        return GraduationStatusSummaryDTO.builder()
                .total(analysisList.size())
                .onTrack(onTrack)
                .atRisk(atRisk)
                .behindSchedule(behind)
                .build();
    }

    @Transactional(readOnly = true)
    public List<DepartmentGraduationStatusDTO> getDepartmentStatus(Long semesterId) {
        List<StudentAnalysis> analysisList = analyzeAllStudents();

        // Group by Department Name
        Map<String, List<StudentAnalysis>> byDept = analysisList.stream()
                .collect(Collectors.groupingBy(StudentAnalysis::getDepartmentName));

        List<DepartmentGraduationStatusDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<StudentAnalysis>> entry : byDept.entrySet()) {
            List<StudentAnalysis> students = entry.getValue();

            long onTrack = students.stream().filter(s -> s.status == StudentStatus.ON_TRACK).count();
            long atRisk = students.stream().filter(s -> s.status == StudentStatus.AT_RISK).count();
            long behind = students.stream().filter(s -> s.status == StudentStatus.BEHIND).count();

            result.add(DepartmentGraduationStatusDTO.builder()
                    .departmentName(entry.getKey())
                    .onTrack(onTrack)
                    .atRisk(atRisk)
                    .behind(behind)
                    .build());
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<CohortAnalyticsDTO> getCohorts(Long semesterId) {
        List<StudentAnalysis> analysisList = analyzeAllStudents();

        // Group by Program Name (Cohort)
        Map<String, List<StudentAnalysis>> byProgram = analysisList.stream()
                .collect(Collectors.groupingBy(StudentAnalysis::getProgramName));

        List<CohortAnalyticsDTO> result = new ArrayList<>();

        for (Map.Entry<String, List<StudentAnalysis>> entry : byProgram.entrySet()) {
            List<StudentAnalysis> students = entry.getValue();
            if (students.isEmpty()) continue;

            String programId = String.valueOf(students.get(0).getProgramId());
            long onTrack = students.stream().filter(s -> s.status == StudentStatus.ON_TRACK).count();
            
            double avgGpa = students.stream()
                    .mapToDouble(StudentAnalysis::getGpa)
                    .average().orElse(0.0);
            
            double avgCompletion = students.stream()
                    .mapToDouble(StudentAnalysis::getCompletionRate)
                    .average().orElse(0.0);

            result.add(CohortAnalyticsDTO.builder()
                    .id(programId)
                    .name(entry.getKey())
                    .total(students.size())
                    .onTrack(onTrack)
                    .avgGpa(Math.round(avgGpa * 100.0) / 100.0)
                    .completionRate(Math.round(avgCompletion * 10.0) / 10.0)
                    .build());
        }

        return result;
    }

    // --- Core Logic ---

    private List<StudentAnalysis> analyzeAllStudents() {
        // 1. Fetch all active students (assuming graduating/active students are the target)
        // You might want to filter by Year Level >= 4 if specifically "Graduating"
        List<Student> students = studentRepository.findAllByIsDeletedFalse(org.springframework.data.domain.Pageable.unpaged()).getContent();
        
        // Filter for only ACTIVE students if needed
        students = students.stream().filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus())).collect(Collectors.toList());

        List<StudentAnalysis> results = new ArrayList<>();

        for (Student student : students) {
            results.add(analyzeStudent(student));
        }

        return results;
    }

    private StudentAnalysis analyzeStudent(Student student) {
        // 1. Get Enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_Id(student.getId().longValue());

        double totalGradePoints = 0.0;
        double totalCreditsEarned = 0.0;
        double totalCreditsAttempted = 0.0;
        boolean hasFailures = false;

        for (Enrollment e : enrollments) {
            if (e.getFinalGrade() == null) continue;

            double credits = e.getCourseOffering().getCourse().getCredits();
            Double gp = GpaCalculationUtil.getGradePoint(e.getFinalGrade());

            totalCreditsAttempted += credits;
            totalGradePoints += (gp * credits);

            if ("F".equalsIgnoreCase(e.getFinalGrade())) {
                hasFailures = true;
            } else {
                totalCreditsEarned += credits;
            }
        }

        double gpa = (totalCreditsAttempted > 0) ? (totalGradePoints / totalCreditsAttempted) : 0.0;
        
        // Calculate Completion Rate based on Program Total
        int requiredCredits = student.getProgram().getTotalCreditRequired();
        double completionRate = (requiredCredits > 0) 
                ? (totalCreditsEarned / requiredCredits) * 100.0 
                : 0.0;
        
        // Cap completion rate at 100%
        if (completionRate > 100.0) completionRate = 100.0;

        // Determine Status
        StudentStatus status;
        if (hasFailures || completionRate < MIN_COMPLETION_RATE_AT_RISK) {
            status = StudentStatus.BEHIND;
        } else if (gpa < MIN_GPA_ON_TRACK || completionRate < MIN_COMPLETION_RATE_ON_TRACK) {
            status = StudentStatus.AT_RISK;
        } else {
            status = StudentStatus.ON_TRACK;
        }

        return new StudentAnalysis(
                student.getId(),
                student.getProgram().getId(),
                student.getProgram().getName(),
                student.getDepartment().getName(),
                gpa,
                completionRate,
                status
        );
    }

    // --- Helper Classes ---

    private enum StudentStatus {
        ON_TRACK, AT_RISK, BEHIND
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class StudentAnalysis {
        private Integer studentId;
        private Long programId;
        private String programName;
        private String departmentName;
        private double gpa;
        private double completionRate;
        private StudentStatus status;
    }
}