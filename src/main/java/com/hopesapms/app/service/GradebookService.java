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

    private final CourseOfferingRepository courseOfferingRepository; 
    private final AssessmentRepository assessmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public GradebookDTO getGradebookForCourseOffering(Long courseOfferingId, Authentication authentication) {

        CourseOffering offering = courseOfferingRepository.findById(courseOfferingId)
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found with id: " + courseOfferingId));

        Course course = offering.getCourse();

        checkUserAuthorityForCourse(authentication, course.getDepartment().getId(), "view gradebook for");

        List<Assessment> assessments = assessmentRepository.findByCourse_IdAndIsDeletedFalse(course.getId());

        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(courseOfferingId);

        List<StudentGradeRowDTO> studentGradeRows = new ArrayList<>();
        for (Enrollment enrollment : enrollments) {
            StudentGradeRowDTO row = new StudentGradeRowDTO();
            row.setEnrollmentId(enrollment.getId());

            Student student = enrollment.getStudent();
            User studentUser = student.getUser();
            
            row.setStudentUserId(studentUser.getId());
            row.setStudentName(studentUser.getFirstName() + " " + studentUser.getLastName());
            row.setStudentId(student.getStudentId());

            List<Score> scores = scoreRepository.findByEnrollmentId(enrollment.getId(), Pageable.unpaged())
                    .getContent();

            Map<Integer, BigDecimal> scoreMap = scores.stream()
                    .collect(Collectors.toMap(s -> s.getAssessment().getId(), Score::getScoreValue));

            BigDecimal totalPercentage = calculateWeightedPercentage(assessments, scoreMap);

            row.setFinalPercentage(totalPercentage.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
            row.setLetterGrade(calculateLetterGrade(totalPercentage));

            studentGradeRows.add(row);
        }

        GradebookDTO gradebook = new GradebookDTO();
        gradebook.setCourseOfferingId(offering.getId());
        gradebook.setCourseName(course.getTitle());
        gradebook.setSectionName(offering.getSection().getName());
        gradebook.setSemesterName(offering.getAcademicSemester().getName());
        
        gradebook.setAssessmentSummaries(assessments.stream()
                .map(a -> new AssessmentSummaryDTO(a.getName(), a.getWeight(), a.getMaxScore()))
                .collect(Collectors.toList()));
        gradebook.setStudentGrades(studentGradeRows);

        return gradebook;
    }

    private BigDecimal calculateWeightedPercentage(List<Assessment> assessments, Map<Integer, BigDecimal> scoreMap) {
        BigDecimal totalPercentage = BigDecimal.ZERO;

        for (Assessment assessment : assessments) {
            BigDecimal score = scoreMap.get(assessment.getId());
            if (score != null) {
                BigDecimal maxScore = assessment.getMaxScore();
                BigDecimal weight = assessment.getWeight();

                BigDecimal percentage = score.divide(maxScore, 4, RoundingMode.HALF_UP)
                        .multiply(weight);

                totalPercentage = totalPercentage.add(percentage);
            }
        }
        return totalPercentage;
    }

    private String calculateLetterGrade(BigDecimal percentage) {

        double p = percentage.doubleValue();
        if (p >= 0.90)
            return "A";
        if (p >= 0.80)
            return "B";
        if (p >= 0.70)
            return "C";
        if (p >= 0.60)
            return "D";
        return "F";
    }

    private void checkUserAuthorityForCourse(Authentication authentication, Long departmentId, String action) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin)
            return;

        boolean isInstructor = user.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));
        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

        if ((isInstructor || isDeptHead) && user.getDepartment() != null
                && user.getDepartment().getId().equals(departmentId)) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to " + action + " this course.");
    }
}