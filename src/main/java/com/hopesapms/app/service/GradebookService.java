package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
    public GradebookDTO getGradebook(Long offeringId) {

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found"));

        // Retrieve assessments for the course
        List<Assessment> assessments = assessmentRepository
                .findByCourse_IdAndIsDeletedFalse(offering.getCourse().getId().intValue());

        // Retrieve enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(offeringId);
        List<Integer> enrollmentIds = enrollments.stream().map(Enrollment::getId).collect(Collectors.toList());

        // Retrieve existing scores
        List<Score> scores = scoreRepository.findByEnrollmentIdIn(enrollmentIds);

        GradebookDTO dto = new GradebookDTO();
        dto.setCourseId(offering.getCourse().getId());
        dto.setCourseName(offering.getCourse().getTitle());
        dto.setSectionName(offering.getSection().getName());

        // Map Columns (Assessments)
        dto.setColumns(assessments.stream().map(a -> {
            GradebookDTO.AssessmentColumnDTO col = new GradebookDTO.AssessmentColumnDTO();
            col.setAssessmentId(a.getId());
            col.setTitle(a.getName());
            col.setMaxScore(a.getMaxScore().doubleValue());
            col.setWeight(a.getWeight().doubleValue());
            return col;
        }).collect(Collectors.toList()));

        // Map Rows (Students & Scores)
        List<GradebookDTO.StudentGradeRowDTO> rows = new ArrayList<>();
        for (Enrollment e : enrollments) {
            Student s = e.getStudent();
            GradebookDTO.StudentGradeRowDTO row = new GradebookDTO.StudentGradeRowDTO();

            row.setEnrollmentId(e.getId());
            row.setStudentIdString(s.getStudentId());
            row.setFullName(s.getUser().getFirstName() + " " + s.getUser().getLastName());

            Map<Integer, Double> scoreMap = new HashMap<>();
            for (Score score : scores) {
                if (score.getEnrollment().getId().equals(e.getId())) {
                    scoreMap.put(score.getAssessment().getId(), score.getScoreValue().doubleValue());
                }
            }
            row.setScores(scoreMap);
            rows.add(row);
        }
        dto.setRows(rows);

        return dto;
    }

    @Transactional
    public void saveScore(SaveScoreRequestDTO dto, Authentication authentication) {

        User grader = getUserFromAuth(authentication);

        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Assessment assessment = assessmentRepository.findById(dto.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        if (dto.getScore() > assessment.getMaxScore().doubleValue()) {
            throw new IllegalArgumentException(
                    "Score " + dto.getScore() + " exceeds max allowed " + assessment.getMaxScore());
        }

        Score score = scoreRepository.findByEnrollmentIdAndAssessmentId(dto.getEnrollmentId(), dto.getAssessmentId())
                .orElse(new Score());

        if (score.getId() == null) {
            score.setEnrollment(enrollment);
            score.setAssessment(assessment);
            score.setCreatedAt(LocalDateTime.now());
        }

        score.setScoreValue(BigDecimal.valueOf(dto.getScore()));
        score.setRecordedBy(grader);
        score.setRecordedDate(LocalDateTime.now());
        score.setUpdatedAt(LocalDateTime.now());

        scoreRepository.save(score);
    }

    private User getUserFromAuth(Authentication authentication) {
        String email = authentication.getName();

        return userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found with email: " + email));
    }
}