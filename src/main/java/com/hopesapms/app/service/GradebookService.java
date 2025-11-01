package com.hopesapms.app.service;

import com.hopesapms.app.dto.AssessmentSummaryDTO;
import com.hopesapms.app.dto.GradebookDTO;
import com.hopesapms.app.dto.StudentGradeRowDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GradebookService {

    private final CourseRepository courseRepository;
    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository; // To get the Student ID

    @Transactional(readOnly = true)
    public GradebookDTO getGradebookForCourse(Integer courseId, Authentication authentication) {
        
        // --- 1. Get Course & Authorize ---
        Course course = courseRepository.findByIdAndIsDeletedFalse(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        checkUserAuthorityForCourse(authentication, course.getDepartment().getId(), "view gradebook for");

        // --- 2. Get All Assessments for the Course ---
        List<Assessment> assessments = assessmentRepository.findByCourse_IdAndIsDeletedFalse(courseId);

        // --- 3. Get All Enrollments for the Course ---
        List<Enrollment> enrollments = enrollmentRepository.findByCourseIdAndIsDeletedFalse(courseId);

        // --- 4. Process Each Student (Enrollment) ---
        List<StudentGradeRowDTO> studentGradeRows = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            StudentGradeRowDTO row = new StudentGradeRowDTO();
            row.setEnrollmentId(enrollment.getId());
            row.setStudentUserId(enrollment.getStudent().getId());
            row.setStudentName(enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName());
            
            // Get the Student ID (e.g., "S12345")
            studentRepository.findByUserId(enrollment.getStudent().getId())
                    .ifPresent(s -> row.setStudentId(s.getStudentId()));

            // Get all scores for this *single* student in this course
            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId(), Pageable.unpaged()).getContent();
            
            // Map scores by AssessmentID for easy lookup
            Map<Integer, BigDecimal> scoreMap = scores.stream()
                    .collect(Collectors.toMap(s -> s.getAssessment().getId(), Score::getScoreValue));
            
            BigDecimal totalPercentage = calculateWeightedPercentage(assessments, scoreMap);
            
            row.setFinalPercentage(totalPercentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            row.setLetterGrade(calculateLetterGrade(totalPercentage));
            
            studentGradeRows.add(row);
        }

        // --- 5. Build Final DTO ---
        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setCourseId(course.getId());
        gradebook.setCourseName(course.getTitle());
        gradebook.setAssessmentSummaries(assessments.stream()
                .map(a -> new AssessmentSummaryDTO(a.getName(), a.getWeight(), a.getMaxScore()))
                .collect(Collectors.toList()));
        gradebook.setStudentGrades(studentGradeRows);

        return gradebook;
    }

    /**
     * Calculates the total weighted percentage for a student.
     * e.g., (score1/max1)*weight1 + (score2/max2)*weight2
     */
    private BigDecimal calculateWeightedPercentage(List<Assessment> assessments, Map<Integer, BigDecimal> scoreMap) {
        BigDecimal totalPercentage = BigDecimal.ZERO;
        
        for (Assessment assessment : assessments) {
            BigDecimal score = scoreMap.get(assessment.getId());
            // Only factor in assessments that have been graded
            if (score != null) {
                BigDecimal maxScore = assessment.getMaxScore();
                BigDecimal weight = assessment.getWeight();
                
                // (score / maxScore) * weight
                BigDecimal percentage = score.divide(maxScore, 4, RoundingMode.HALF_UP)
                                             .multiply(weight);
                
                totalPercentage = totalPercentage.add(percentage);
            }
        }
        return totalPercentage;
    }

    /**
     * Simple hard-coded grading scale.
     * This could be moved to a database table later (GradingScale).
     */
    private String calculateLetterGrade(BigDecimal percentage) {
        // percentage is 0.0 to 1.0
        double p = percentage.doubleValue();
        if (p >= 0.90) return "A";
        if (p >= 0.80) return "B";
        if (p >= 0.70) return "C";
        if (p >= 0.60) return "D";
        return "F";
    }

    private void checkUserAuthorityForCourse(Authentication authentication, Long departmentId, String action) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin) return;

        boolean isInstructor = user.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));
        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));
        
        if ((isInstructor || isDeptHead) && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return; // Authorized
        }
        throw new AccessDeniedException("You do not have permission to " + action + " this course.");
    }
}