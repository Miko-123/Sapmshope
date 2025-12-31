package com.hopesapms.app.service;

import com.hopesapms.app.dto.AssessmentRequestDTO;
import com.hopesapms.app.dto.AssessmentResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Assessment;
import com.hopesapms.app.model.CourseOffering;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.AssessmentRepository;
import com.hopesapms.app.repository.CourseOfferingRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final CourseOfferingRepository courseOfferingRepository; 
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public AssessmentResponseDTO createAssessment(AssessmentRequestDTO dto, Authentication authentication) {
        CourseOffering initialOffering = courseOfferingRepository.findById(dto.getCourseOfferingId()) 
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found"));

        User user = checkUserAuthorityForCourse(authentication, initialOffering.getCourse().getDepartment().getId(), "create assessment for");

        boolean isBaseRequest = dto.getIsBase() != null && dto.getIsBase();
        
        if (isBaseRequest && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("Only Department Heads or Admins can create 'Base' assessments.");
        }

        List<CourseOffering> targetOfferings = new ArrayList<>();
        
        if (!isBaseRequest && initialOffering.getInstructor() != null) {
             targetOfferings = courseOfferingRepository.findByAcademicSemester_IdAndCourse_IdAndInstructor_IdAndIsDeletedFalse(
                    initialOffering.getAcademicSemester().getId(),
                    initialOffering.getCourse().getId(),
                    initialOffering.getInstructor().getId()
            );
        } else {
            targetOfferings.add(initialOffering);
        }

        if (targetOfferings.isEmpty()) {
            targetOfferings.add(initialOffering);
        }

        Assessment createdAssessmentForInitialOffering = null;

        for (CourseOffering offering : targetOfferings) {
            List<Assessment> existingAssessments = assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(offering.getId());
            boolean exists = existingAssessments.stream()
                    .anyMatch(a -> a.getName().equalsIgnoreCase(dto.getName()));
            
            if (exists) {
                if (offering.getId().equals(initialOffering.getId())) {
                    createdAssessmentForInitialOffering = existingAssessments.stream()
                        .filter(a -> a.getName().equalsIgnoreCase(dto.getName()))
                        .findFirst().orElse(null);
                }
                continue; 
            }

            validateAssessmentWeight(offering.getId(), dto.getWeight(), null);

            Assessment assessment = Assessment.builder()
                    .courseOffering(offering) 
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

            if (offering.getId().equals(initialOffering.getId())) {
                createdAssessmentForInitialOffering = saved;
            }
        }

        if (createdAssessmentForInitialOffering == null) {
             throw new IllegalArgumentException("Assessment already exists for this course.");
        }

        return mapToResponseDTO(createdAssessmentForInitialOffering);
    }

    @Transactional
    public AssessmentResponseDTO updateAssessment(Long id, AssessmentRequestDTO dto, Authentication authentication) {
        
        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(id.intValue()) 
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        User user = checkUserAuthorityForCourse(authentication, assessment.getCourseOffering().getCourse().getDepartment().getId(), "update");

        if (assessment.isBase() && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("You cannot modify a Core Course Assessment defined by the Department.");
        }
        validateAssessmentWeight(assessment.getCourseOffering().getId(), dto.getWeight(), id.intValue());

        String oldData = assessment.toString();

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
    public Page<AssessmentResponseDTO> getAssessmentsForOffering(Long offeringId, Pageable pageable) {
        if (!courseOfferingRepository.existsById(offeringId)) {
            throw new ResourceNotFoundException("Course Offering not found with id: " + offeringId);
        }
        
        return assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(offeringId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Transactional
    public void deleteAssessment(Long id, Authentication authentication) {

        Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(id.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found"));

        User user = checkUserAuthorityForCourse(authentication, assessment.getCourseOffering().getCourse().getDepartment().getId(), "delete");

        if (assessment.isBase() && !isDeptHeadOrAdmin(user)) {
            throw new AccessDeniedException("You cannot delete a Core Course Assessment defined by the Department.");
        }

        String oldData = assessment.toString();
        assessment.setDeleted(true);
        assessmentRepository.save(assessment);

        auditLogService.log("DELETE_ASSESSMENT", "Assessment", id, oldData, "DELETED");
    }

    @Transactional(readOnly = true)
    public List<AssessmentResponseDTO> getMyAssessments(Authentication authentication) {
        User user = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Page<Enrollment> enrollments = enrollmentRepository.findByStudentId(user.getId(), Pageable.unpaged());

        Stream<Assessment> allAssessments = enrollments.stream()
                .filter(e -> "ENROLLED".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus()))
                .flatMap(enrollment -> {
                    return assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(
                            enrollment.getCourseOffering().getId()
                    ).stream();
                });

        return allAssessments
                .distinct()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private void validateAssessmentWeight(Long offeringId, BigDecimal newWeight, Integer assessmentToIgnoreId) {
        List<Assessment> existingAssessments = assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(offeringId);
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
        User user = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
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
        
        if (entity.getCourseOffering() != null) {
            dto.setCourseId(entity.getCourseOffering().getCourse().getId()); 
            dto.setCourseOfferingId(entity.getCourseOffering().getId());
        }
        
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