package com.hopesapms.app.service;

import com.hopesapms.app.dto.CreateGradeChangeRequest;
import com.hopesapms.app.dto.GradeChangeRequestDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GradeChangeService {

    private final GradeChangeRequestRepository requestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService; // ✅ Injected
    private final AuditLogService auditLogService;

    @Transactional
    public GradeChangeRequestDTO requestChange(CreateGradeChangeRequest dto, Authentication authentication) {
        User instructor = getUser(authentication);

        Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId().intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        Assessment assessment = assessmentRepository.findById(dto.getAssessmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        Optional<Score> currentScoreOpt = scoreRepository.findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(
                enrollment.getId().intValue(), assessment.getId());

        BigDecimal currentScoreVal = currentScoreOpt.map(Score::getScoreValue).orElse(BigDecimal.ZERO);

        GradeChangeRequest request = GradeChangeRequest.builder()
                .enrollment(enrollment)
                .assessment(assessment)
                .oldScore(currentScoreVal.doubleValue())
                .newScore(dto.getNewScore())
                .reason(dto.getReason())
                .status("PENDING")
                .requestedBy(instructor)
                .build();

        GradeChangeRequest saved = requestRepository.save(request);

        Department dept = enrollment.getCourseOffering().getCourse().getDepartment();
        if (dept != null) {

            List<User> deptHeads = userRepository.findByDepartmentIdAndRoleName(dept.getId(), "DEPARTMENT_HEAD");
            for (User head : deptHeads) {
                notificationService.createNotification(
                        head.getId(),
                        "New Grade Change Request",
                        "Instructor " + instructor.getFirstName() + " requested a grade change for " +
                                enrollment.getCourseOffering().getCourse().getCourseCode() + ".",
                        "GRADE_REQUEST");
            }
        }

        return mapToDTO(saved);
    }

    @Transactional
    public GradeChangeRequestDTO approveRequest(Long requestId, String adminComment, Authentication authentication) {
        User admin = getUser(authentication);
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is already " + request.getStatus());
        }

        Score score = scoreRepository.findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(
                request.getEnrollment().getId().intValue(),
                request.getAssessment().getId()).orElse(
                        Score.builder()
                                .enrollment(request.getEnrollment())
                                .assessment(request.getAssessment())
                                .isDeleted(false)
                                .build());

        score.setScoreValue(BigDecimal.valueOf(request.getNewScore()));
        scoreRepository.save(score);

        request.setStatus("APPROVED");
        request.setHandledBy(admin);
        request.setAdminComments(adminComment);
        GradeChangeRequest saved = requestRepository.save(request);

        auditLogService.log("APPROVE_GRADE_CHANGE", "Score", score.getId().longValue(),
                "Old: " + request.getOldScore(), "New: " + request.getNewScore());

        notificationService.createNotification(
                request.getRequestedBy().getId(),
                "Grade Request Approved",
                "Your request for " + request.getEnrollment().getStudent().getStudentId() +
                        " in " + request.getEnrollment().getCourseOffering().getCourse().getCourseCode() +
                        " has been approved.",
                "GRADE_APPROVED");

        return mapToDTO(saved);
    }

    @Transactional
    public GradeChangeRequestDTO rejectRequest(Long requestId, String adminComment, Authentication authentication) {
        User admin = getUser(authentication);
        GradeChangeRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Request is already " + request.getStatus());
        }

        request.setStatus("REJECTED");
        request.setHandledBy(admin);
        request.setAdminComments(adminComment);

        GradeChangeRequest saved = requestRepository.save(request);

        notificationService.createNotification(
                request.getRequestedBy().getId(),
                "Grade Request Rejected",
                "Your request for " + request.getEnrollment().getStudent().getStudentId() +
                        " was rejected. Reason: " + (adminComment != null ? adminComment : "No reason provided."),
                "GRADE_REJECTED");

        return mapToDTO(saved);
    }

    @Transactional(readOnly = true)
    public Page<GradeChangeRequestDTO> getPendingRequests(Pageable pageable) {
        return requestRepository.findByStatus("PENDING", pageable).map(this::mapToDTO);
    }

    private User getUser(Authentication authentication) {
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private GradeChangeRequestDTO mapToDTO(GradeChangeRequest entity) {
        GradeChangeRequestDTO dto = new GradeChangeRequestDTO();
        dto.setId(entity.getId());
        dto.setEnrollmentId(entity.getEnrollment().getId().longValue());
        dto.setStudentName(entity.getEnrollment().getStudent().getUser().getFirstName() + " "
                + entity.getEnrollment().getStudent().getUser().getLastName());
        dto.setStudentId(entity.getEnrollment().getStudent().getStudentId());
        dto.setCourseCode(entity.getEnrollment().getCourseOffering().getCourse().getCourseCode());
        dto.setAssessmentId(entity.getAssessment().getId());
        dto.setAssessmentName(entity.getAssessment().getName());
        dto.setOldScore(entity.getOldScore());
        dto.setNewScore(entity.getNewScore());
        dto.setReason(entity.getReason());
        dto.setStatus(entity.getStatus());
        dto.setInstructorName(entity.getRequestedBy().getFirstName() + " " + entity.getRequestedBy().getLastName());
        dto.setCreatedAt(entity.getCreatedAt());

        if (entity.getHandledBy() != null) {
            dto.setHandledByName(entity.getHandledBy().getFirstName());
            dto.setAdminComments(entity.getAdminComments());
        }
        return dto;
    }
}