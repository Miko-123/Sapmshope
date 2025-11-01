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
    private final AuditLogService auditLogService;

    @Transactional
    public ScoreResponseDTO enterScore(ScoreRequestDTO dto, Authentication authentication) {
        
        // --- 1. Get Entities ---
        User instructor = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found with id: " + dto.getEnrollmentId()));

        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(dto.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + dto.getAssessmentId()));

        // --- 2. Authorization Check ---
        // Can this instructor grade this assessment?
        checkUserAuthorityForCourse(instructor, assessment.getCourse().getDepartment().getId(), "enter score for");

        // --- 3. Business Logic: Validate Score ---
        if (dto.getScoreValue().compareTo(assessment.getMaxScore()) > 0) {
            throw new IllegalArgumentException("Score (" + dto.getScoreValue() + ") cannot be greater than the assessment's max score (" + assessment.getMaxScore() + ")");
        }

        // --- 4. Create or Update Logic ---
        Optional<Score> existingScoreOpt = scoreRepository
                .findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(dto.getEnrollmentId(), dto.getAssessmentId());

        Score score;
        String oldData = null;
        String action = "ENTER_SCORE";

        if (existingScoreOpt.isPresent()) {
            // --- UPDATE ---
            score = existingScoreOpt.get();
            oldData = score.toString();
            action = "UPDATE_SCORE";

            score.setOriginalScoreValue(score.getScoreValue()); // Archive old score
            score.setScoreValue(dto.getScoreValue());
            score.setOverriden(true);
            
        } else {
            // --- CREATE ---
            score = Score.builder()
                    .enrollment(enrollment)
                    .assessment(assessment)
                    .scoreValue(dto.getScoreValue())
                    .isOverriden(false)
                    .build();
        }
        
        score.setRecordedBy(instructor);
        score.setRecordedDate(LocalDateTime.now());

        Score savedScore = scoreRepository.save(score);
        auditLogService.log(action, "Score", savedScore.getId().longValue(), oldData, savedScore.toString());

        return mapToResponseDTO(savedScore);
    }

    @Transactional
    public BulkUploadResponse bulkEnterScores(BulkScoreRequestDTO bulkDto, Authentication authentication) {
        
        User instructor = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(bulkDto.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found with id: " + bulkDto.getAssessmentId()));
        
        checkUserAuthorityForCourse(instructor, assessment.getCourse().getDepartment().getId(), "bulk enter scores for");

        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (BulkScoreRequestDTO.StudentScoreEntry entry : bulkDto.getScores()) {
            try {
                // Build a single DTO to reuse the single-entry logic
                ScoreRequestDTO singleDto = new ScoreRequestDTO();
                singleDto.setEnrollmentId(entry.getEnrollmentId());
                singleDto.setAssessmentId(bulkDto.getAssessmentId());
                singleDto.setScoreValue(entry.getScoreValue());

                // Call the existing, validated method
                enterScore(singleDto, authentication);
                successCount++;
                
            } catch (Exception e) {
                failedCount++;
                errors.add("EnrollmentID " + entry.getEnrollmentId() + ": FAILED | Error: " + e.getMessage());
            }
        }
        
        BulkUploadResponse response = new BulkUploadResponse();
        response.setSuccessCount(successCount);
        response.setFailedCount(failedCount);
        response.setErrors(errors);
        
        auditLogService.log("BULK_ENTER_SCORES", "Score", assessment.getId().longValue(), null, "Success: " + successCount + ", Failed: " + failedCount);
        return response;
    }

    
    private void checkUserAuthorityForCourse(User user, Long departmentId, String action) {
        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin) return;

        boolean isInstructor = user.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));
        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));
        
        if ((isInstructor || isDeptHead) && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return; 
        }
        throw new AccessDeniedException("You do not have permission to " + action + " this course.");
    }

    @Transactional(readOnly = true)
    public List<StudentScoreDTO> getMyScoresForCourse(Integer courseId, Authentication authentication) {
        
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Page<Score> scores = scoreRepository.findByStudentAndCourse(user.getId(), courseId, Pageable.unpaged());

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
        dto.setRecordedDate(s.getRecordedDate());
        dto.setRecordedBy(s.getRecordedBy().getUsername());
        
        return dto;
    }
    
    private ScoreResponseDTO mapToResponseDTO(Score s) {
        ScoreResponseDTO dto = new ScoreResponseDTO();
        dto.setId(s.getId());
        dto.setEnrollmentId(s.getEnrollment().getId());
        dto.setAssessmentId(s.getAssessment().getId());
        dto.setScoreValue(s.getScoreValue());
        dto.setRecordedByUsername(s.getRecordedBy().getUsername());
        dto.setRecordedDate(s.getRecordedDate());
        dto.setOverriden(s.isOverriden());
        dto.setOriginalScoreValue(s.getOriginalScoreValue());
        return dto;
    }
}