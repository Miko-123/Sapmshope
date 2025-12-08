package com.hopesapms.app.service;

import com.hopesapms.app.dto.BulkScoreRequestDTO;
import com.hopesapms.app.dto.BulkUploadResponse; // Re-using this DTO
import com.hopesapms.app.dto.ScoreRequestDTO;
import com.hopesapms.app.dto.ScoreResponseDTO;
import com.hopesapms.app.dto.StudentScoreDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScoreService {

        private final ScoreRepository scoreRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final AssessmentRepository assessmentRepository;
        private final UserRepository userRepository;
        private final StudentRepository studentRepository;
        private final AuditLogService auditLogService;
        private final InstructorRepository instructorRepository;

        @Transactional
        public List<ScoreResponseDTO> enterScore(List<ScoreRequestDTO> dtoList, Authentication authentication) {

                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String username = auth.getName();

                Instructor instructor = instructorRepository.findByUserUsername(username)
                                .orElseThrow(() -> new RuntimeException("Instructor not found for current user"));

                List<ScoreResponseDTO> responses = new ArrayList<>();

                for (ScoreRequestDTO dto : dtoList) {

                        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Enrollment not found with id: " + dto.getEnrollmentId()));

                        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(dto.getAssessmentId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Assessment not found with id: " + dto.getAssessmentId()));

                        checkUserAuthorityForCourse(
                                        instructor,
                                        assessment.getCourse().getDepartment().getId(),
                                        "enter score for");

                        if (dto.getScoreValue().compareTo(assessment.getMaxScore()) > 0) {
                                throw new IllegalArgumentException(
                                                "Score (" + dto.getScoreValue() + ") cannot exceed max score ("
                                                                + assessment.getMaxScore() + ")");
                        }

                        Score saved = saveOrUpdateScore(enrollment, assessment, dto.getScoreValue(), instructor);

                        responses.add(mapToResponseDTO(saved));
                }

                return responses;
        }

        @Transactional(readOnly = true)
        public Page<ScoreResponseDTO> getSectionScores(
                        Integer sectionId,
                        String courseTitle,
                        Pageable pageable) {
                String username = SecurityContextHolder.getContext().getAuthentication().getName();

                return scoreRepository.getStudentScores(sectionId, username, courseTitle, pageable);
        }

        // Changed the how checkUserAuthorityForCourse works, instead of checking for
        // the user it checks for the instructor i believe, havent tested yet
        @Transactional
        public BulkUploadResponse bulkEnterScores(BulkScoreRequestDTO bulkDto, Authentication authentication) {

                Instructor instructor = instructorRepository
                                .findByUserUsernameAndUserIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

                Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(bulkDto.getAssessmentId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assessment not found with id: " + bulkDto.getAssessmentId()));

                checkUserAuthorityForCourse(instructor, assessment.getCourse().getDepartment().getId(),
                                "bulk enter scores for");

                List<String> errors = new ArrayList<>();
                int successCount = 0;
                int failedCount = 0;

                for (BulkScoreRequestDTO.StudentScoreEntry entry : bulkDto.getScores()) {
                        try {
                                ScoreRequestDTO singleDto = new ScoreRequestDTO();
                                singleDto.setEnrollmentId(entry.getEnrollmentId());
                                singleDto.setAssessmentId(bulkDto.getAssessmentId());
                                singleDto.setScoreValue(entry.getScoreValue());

                                enterScore(singleDto, authentication);
                                successCount++;

                        } catch (Exception e) {
                                failedCount++;
                                errors.add("EnrollmentID " + entry.getEnrollmentId() + ": FAILED | Error: "
                                                + e.getMessage());
                        }
                }

                BulkUploadResponse response = new BulkUploadResponse();
                response.setSuccessCount(successCount);
                response.setFailedCount(failedCount);
                response.setErrors(errors);

                auditLogService.log("BULK_ENTER_SCORES", "Score", assessment.getId().longValue(), null,
                                "Success: " + successCount + ", Failed: " + failedCount);
                return response;
        }

        // Change this so that only the instructor assigned to the course can
        // enter/update scores
        private void checkUserAuthorityForCourse(Instructor instructor, Long departmentId, String action) {
                boolean isSystemAdmin = instructor.getUser().getRoles().stream()
                                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
                if (isSystemAdmin)
                        return;

                boolean isInstructor = instructor.getUser().getRoles().stream()
                                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));
                boolean isDeptHead = instructor.getUser().getRoles().stream()
                                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

                if ((isInstructor || isDeptHead) && instructor.getUser().getDepartment() != null
                                && instructor.getUser().getDepartment().getId().equals(departmentId)) {
                        return;
                }
                throw new AccessDeniedException("You do not have permission to " + action + " this course.");
        }

        @Transactional(readOnly = true)
        public List<StudentScoreDTO> getMyScoresForCourse(Integer courseId, Authentication authentication) {

                User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

                Student student = studentRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new AccessDeniedException("This user is not a student."));

                Page<Score> scores = scoreRepository.findByStudentAndCourse(student.getId(), courseId,
                                Pageable.unpaged());

                return scores.stream()
                                .map(this::mapToStudentScoreDTO)
                                .collect(Collectors.toList());
        }

        private StudentScoreDTO mapToStudentScoreDTO(Score s) {
                StudentScoreDTO dto = new StudentScoreDTO();

                Assessment assessment = s.getAssessment();
                dto.setAssessmentName(assessment.getName());
                dto.setAssessmentType(assessment.getType());
                dto.setMaxScore(assessment.getMaxScore());

                dto.setScoreId(s.getId());
                dto.setScoreValue(s.getScoreValue());

                // dto.setRecordedBy(s.getRecordedBy().getUsername());

                return dto;
        }

        private ScoreResponseDTO mapToResponseDTO(Score s) {
                ScoreResponseDTO dto = new ScoreResponseDTO();
                dto.setId(s.getId());
                dto.setEnrollmentId(s.getEnrollment().getId());
                dto.setAssessmentId(s.getAssessment().getId());
                dto.setScoreValue(s.getScoreValue());
                // dto.setRecordedByUsername(s.getRecordedBy().getUsername());
                dto.setOverriden(s.isOverriden());
                dto.setOriginalScoreValue(s.getOriginalScoreValue());
                return dto;
        }

        private Score saveOrUpdateScore(
                        Enrollment enrollment,
                        Assessment assessment,
                        BigDecimal scoreValue,
                        Instructor instructor) {
                Optional<Score> existingScore = scoreRepository.findByEnrollmentAndAssessment(enrollment, assessment);

                Score score;

                if (existingScore.isPresent()) {
                        score = existingScore.get();
                        score.setScoreValue(scoreValue);
                        score.setUpdatedAt(LocalDateTime.now());
                } else {
                        score = new Score();
                        score.setEnrollment(enrollment);
                        score.setAssessment(assessment);
                        score.setScoreValue(scoreValue);
                        score.setRecordedBy(instructor);
                        score.setCreatedAt(LocalDateTime.now());
                        score.setRecordedDate(LocalDateTime.now());
                }

                return scoreRepository.save(score);

        }
}