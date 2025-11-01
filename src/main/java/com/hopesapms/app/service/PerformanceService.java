package com.hopesapms.app.service;

import com.hopesapms.app.dto.CoursePerformanceDTO;
import com.hopesapms.app.dto.SemesterPerformanceDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import com.hopesapms.app.util.GpaCalculationUtil; // Import our new util
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
public class PerformanceService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;

    @Transactional(readOnly = true)
    public SemesterPerformanceDTO getMyPerformance(Authentication authentication) {
        
        // --- 1. Get User and All Enrollments ---
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        // Get ALL enrollments (active and completed)
        List<Enrollment> allEnrollments = enrollmentRepository.findByStudentId(user.getId(), Pageable.unpaged()).getContent();

        List<CoursePerformanceDTO> coursePerformances = new ArrayList<>();
        double totalQualityPoints = 0.0;
        double totalCreditsAttempted = 0.0;
        double totalCreditsEarned = 0.0;
        
        // --- 2. Loop Through Each Enrollment ---
        for (Enrollment enrollment : allEnrollments) {
            Course course = enrollment.getCourse();
            
            // Get all assessments for this single course
            List<Assessment> assessments = assessmentRepository.findByCourse_IdAndIsDeletedFalse(course.getId());
            
            // Get all scores for this single enrollment
            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId(), Pageable.unpaged()).getContent();
            Map<Integer, BigDecimal> scoreMap = scores.stream()
                    .collect(Collectors.toMap(s -> s.getAssessment().getId(), Score::getScoreValue));

            // --- 3. Calculate Final Grade for this Course ---
            BigDecimal finalPercentage = calculateWeightedPercentage(assessments, scoreMap);
            String letterGrade = GpaCalculationUtil.calculateLetterGrade(finalPercentage);
            Double gradePoint = GpaCalculationUtil.getGradePoint(letterGrade);
            Double courseCredits = course.getCredits(); // Assuming Course model has getCredits()

            // --- 4. Populate DTO and aggregate totals ---
            CoursePerformanceDTO courseDTO = new CoursePerformanceDTO();
            courseDTO.setCourseCode(course.getCourseCode());
            courseDTO.setCourseTitle(course.getTitle());
            courseDTO.setCredits(courseCredits);
            courseDTO.setFinalPercentage(finalPercentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            courseDTO.setLetterGrade(letterGrade);
            courseDTO.setGradePoint(gradePoint);
            coursePerformances.add(courseDTO);

            // --- 5. GPA Calculation Logic ---
            if (courseCredits != null) {
                totalCreditsAttempted += courseCredits;
                // Quality Points = Grade Point * Course Credits
                totalQualityPoints += (gradePoint * courseCredits);
                if (!"F".equals(letterGrade)) {
                    totalCreditsEarned += courseCredits;
                }
            }
        }
        
        // --- 6. Final Calculation & DTO Assembly ---
        // CGPA = Total Quality Points / Total Credits Attempted
        double cgpa = (totalCreditsAttempted == 0) ? 0.0 : (totalQualityPoints / totalCreditsAttempted);
        
        SemesterPerformanceDTO performanceDTO = new SemesterPerformanceDTO();
        performanceDTO.setCourses(coursePerformances);
        performanceDTO.setTotalCreditsAttempted(totalCreditsAttempted);
        performanceDTO.setTotalCreditsEarned(totalCreditsEarned);
        
        // For now, CGPA is the same as Semester GPA since we don't have semester filtering
        performanceDTO.setSemesterGPA(cgpa); 
        performanceDTO.setCumulativeGPA(cgpa);

        return performanceDTO;
    }

    /**
     * This logic is duplicated from GradebookService.
     * It should be moved to a shared service, but is here for simplicity.
     */
    private BigDecimal calculateWeightedPercentage(List<Assessment> assessments, Map<Integer, BigDecimal> scoreMap) {
        BigDecimal totalPercentage = BigDecimal.ZERO;
        for (Assessment assessment : assessments) {
            BigDecimal score = scoreMap.get(assessment.getId());
            if (score != null) {
                BigDecimal maxScore = assessment.getMaxScore();
                BigDecimal weight = assessment.getWeight();
                BigDecimal percentage = score.divide(maxScore, 4, RoundingMode.HALF_UP).multiply(weight);
                totalPercentage = totalPercentage.add(percentage);
            }
        }
        return totalPercentage;
    }
}