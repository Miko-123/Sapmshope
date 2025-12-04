package com.hopesapms.app.service;

import com.hopesapms.app.dto.CoursePerformanceDTO;
import com.hopesapms.app.dto.SemesterPerformanceDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import com.hopesapms.app.util.GpaCalculationUtil; 
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
        
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        List<Enrollment> allEnrollments = enrollmentRepository.findByStudentId(user.getId(), Pageable.unpaged()).getContent();

        List<CoursePerformanceDTO> coursePerformances = new ArrayList<>();
        double totalQualityPoints = 0.0;
        double totalCreditsAttempted = 0.0;
        double totalCreditsEarned = 0.0;
        

        for (Enrollment enrollment : allEnrollments) {
            CourseOffering offering = enrollment.getCourseOffering();
            Course course = offering.getCourse();
            
           
            List<Assessment> assessments = assessmentRepository.findByCourse_IdAndIsDeletedFalse(course.getId());
            
            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId(), Pageable.unpaged()).getContent();
            Map<Integer, BigDecimal> scoreMap = scores.stream()
                    .collect(Collectors.toMap(s -> s.getAssessment().getId(), Score::getScoreValue));

            BigDecimal finalPercentage = calculateWeightedPercentage(assessments, scoreMap);
            String letterGrade = GpaCalculationUtil.calculateLetterGrade(finalPercentage);
            Double gradePoint = GpaCalculationUtil.getGradePoint(letterGrade);
            Double courseCredits = course.getCredits(); 

            CoursePerformanceDTO courseDTO = new CoursePerformanceDTO();
            courseDTO.setCourseCode(course.getCourseCode());
            courseDTO.setCourseTitle(course.getTitle());
            courseDTO.setCredits(courseCredits);
            courseDTO.setFinalPercentage(finalPercentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            courseDTO.setLetterGrade(letterGrade);
            courseDTO.setGradePoint(gradePoint);
            coursePerformances.add(courseDTO);

            if (courseCredits != null) {
                totalCreditsAttempted += courseCredits;
                totalQualityPoints += (gradePoint * courseCredits);
                if (!"F".equals(letterGrade)) {
                    totalCreditsEarned += courseCredits;
                }
            }
        }
        
        double cgpa = (totalCreditsAttempted == 0) ? 0.0 : (totalQualityPoints / totalCreditsAttempted);
        
        SemesterPerformanceDTO performanceDTO = new SemesterPerformanceDTO();
        performanceDTO.setCourses(coursePerformances);
        performanceDTO.setTotalCreditsAttempted(totalCreditsAttempted);
        performanceDTO.setTotalCreditsEarned(totalCreditsEarned);
        
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