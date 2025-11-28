package com.hopesapms.app.service;

import com.hopesapms.app.dto.AssessmentRequestDTO;
import com.hopesapms.app.dto.AssessmentResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Assessment;
import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.AssessmentRepository;
import com.hopesapms.app.repository.CourseRepository;
import com.hopesapms.app.repository.EnrollmentRepository;
import com.hopesapms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AssessmentResponseDTO createAssessment(AssessmentRequestDTO dto, Authentication authentication) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        User user = userRepository.findByIdAndIsDeletedFalse(dto.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // --- 1. Authorization Check ---
        checkUserAuthorityForCourse(authentication, course.getDepartment().getId(), "create assessment for");

        // --- 2. Check Weight ---
        validateAssessmentWeight(dto.getCourseId(), dto.getWeight(), null);

        boolean isBaseRequest = dto.getIsBase() != null && dto.getIsBase();

        if (isBaseRequest && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("Only Department Heads or Admins can create 'Base' assessments.");
        }
        Assessment assessment = Assessment.builder()
                .userId(user)
                .course(course)
                .name(dto.getName())
                .type(dto.getType())
                .maxScore(dto.getMaxScore())
                .weight(dto.getWeight())
                .dueDate(dto.getDueDate())
                .description(dto.getDescription())
                .status("PENDING")
                .isBase(isBaseRequest)
                .build();

        Assessment saved = assessmentRepository.save(assessment);
        auditLogService.log("CREATE_ASSESSMENT", "Assessment", saved.getId().longValue(), null, saved.toString());

        return mapToResponseDTO(saved);
    }

    @Transactional
    public AssessmentResponseDTO updateAssessment(Integer id, AssessmentRequestDTO dto, Authentication authentication) {
        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        User user = checkUserAuthorityForCourse(authentication, assessment.getCourse().getDepartment().getId(),
                "update");

        if (assessment.isBase() && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("You cannot modify a Core Course Assessment defined by the Department.");
        }

        validateAssessmentWeight(assessment.getCourse().getId(), dto.getWeight(), id);

        String oldData = assessment.toString();

        assessment.setUserId(assessment.getUserId());
        assessment.setName(dto.getName());
        assessment.setType(dto.getType());
        assessment.setMaxScore(dto.getMaxScore());
        assessment.setWeight(dto.getWeight());
        assessment.setDueDate(dto.getDueDate());
        assessment.setDescription(dto.getDescription());

        if (dto.getIsBase() != null && isDeptHeadOrAdmin(user)) {
            assessment.setBase(dto.getIsBase());
        }

        Assessment updated = assessmentRepository.save(assessment);
        auditLogService.log("UPDATE_ASSESSMENT", "Assessment", updated.getId().longValue(), oldData,
                updated.toString());

        return mapToResponseDTO(updated);
    }

    @Transactional(readOnly = true)
    public Page<AssessmentResponseDTO> getAssessmentsForCourse(Integer courseId, Pageable pageable) {

        if (!courseRepository.existsByIdAndIsDeletedFalse(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }

        return assessmentRepository.findByCourseId(courseId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional
    public void deleteAssessment(Integer id, Authentication authentication) {
        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        User user = checkUserAuthorityForCourse(authentication, assessment.getCourse().getDepartment().getId(),
                "delete");

        if (assessment.isBase() && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("You cannot delete a Core Course Assessment defined by the Department.");
        }

        String oldData = assessment.toString();
        assessment.setDeleted(true);
        assessmentRepository.save(assessment);

        auditLogService.log("DELETE_ASSESSMENT", "Assessment", id.longValue(), oldData, "DELETED");
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponseDTO> getMyAssessments(Authentication authentication) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Page<Enrollment> enrollments = enrollmentRepository.findByStudentId(user.getId(), Pageable.unpaged());

        Stream<Assessment> allAssessments = enrollments.stream()
                .filter(e -> "ENROLLED".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus()))
                .flatMap(enrollment -> {
                    return assessmentRepository.findByCourse_IdAndIsDeletedFalse(
                            enrollment.getCourseOffering().getCourse().getId()).stream();
                });

        return allAssessments
                .distinct()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateAssessmentWeight(Integer courseId, BigDecimal newWeight, Integer assessmentToIgnoreId) {
        List<Assessment> existingAssessments = assessmentRepository.findByCourse_IdAndIsDeletedFalse(courseId);
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (Assessment existing : existingAssessments) {
            if (assessmentToIgnoreId != null && existing.getId().equals(assessmentToIgnoreId)) {
                continue;
            }
            totalWeight = totalWeight.add(existing.getWeight());
        }
        totalWeight = totalWeight.add(newWeight);

        if (totalWeight.compareTo(new BigDecimal("1.0001")) > 0) {
            throw new IllegalArgumentException("Total course weight cannot exceed 100%. Current total with new item: " +
                    totalWeight.multiply(BigDecimal.valueOf(100)).setScale(2) + "%");
        }
    }

    private User checkUserAuthorityForCourse(Authentication authentication, Long departmentId, String action) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        if (isDeptHeadOrAdmin(user)) {
            return user;
        }

        boolean isInstructor = user.getRoles().stream()
                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));

        if (isInstructor && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return user;
        }

        throw new AccessDeniedException("You do not have permission to " + action + " this course.");
    }

    private boolean isDeptHeadOrAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()) || "DEPARTMENT_HEAD".equals(role.getName()));
    }

    private AssessmentResponseDTO mapToResponseDTO(Assessment entity) {
        AssessmentResponseDTO dto = new AssessmentResponseDTO();
        dto.setId(entity.getId());
        dto.setCourseId(entity.getCourse().getId());
        dto.setName(entity.getName());
        dto.setType(entity.getType());
        dto.setMaxScore(entity.getMaxScore());
        dto.setWeight(entity.getWeight());
        dto.setDueDate(entity.getDueDate());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setBase(entity.isBase());
        return dto;
    }
}