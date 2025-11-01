package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseSummaryDTO;
import com.hopesapms.app.dto.PrerequisiteRequestDTO;
import com.hopesapms.app.dto.PrerequisiteResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.Prerequisite;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.CourseRepository; // You will need to create this
import com.hopesapms.app.repository.PrerequisiteRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrerequisiteService {

    private final PrerequisiteRepository prerequisiteRepository;
    private final CourseRepository courseRepository; // Assumes you have this
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public PrerequisiteResponseDTO createPrerequisite(PrerequisiteRequestDTO dto, Authentication authentication) {
        
        // --- 1. Get Entities ---
        Course mainCourse = courseRepository.findByIdAndIsDeletedFalse(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Main course not found with id: " + dto.getCourseId()));

        Course prereqCourse = courseRepository.findByIdAndIsDeletedFalse(dto.getPrerequisiteCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisite course not found with id: " + dto.getPrerequisiteCourseId()));

        // --- 2. Authorization Check ---
        checkUserAuthorityForCourse(authentication, mainCourse.getDepartment().getId(), "create");
        
        // --- 3. Business Logic ---
        if (mainCourse.getId().equals(prereqCourse.getId())) {
            throw new IllegalArgumentException("A course cannot be a prerequisite for itself.");
        }

        if (prerequisiteRepository.existsByCourse_IdAndPrerequisiteCourse_IdAndIsDeletedFalse(mainCourse.getId(), prereqCourse.getId())) {
            throw new EntityExistsException("This prerequisite relationship already exists.");
        }

        // --- 4. Create and Save ---
        Prerequisite prerequisite = Prerequisite.builder()
                .course(mainCourse)
                .prerequisiteCourse(prereqCourse)
                .minGrade(dto.getMinGrade())
                .isRequired(dto.getIsRequired() != null ? dto.getIsRequired() : true)
                .build();

        Prerequisite savedPrereq = prerequisiteRepository.save(prerequisite);

        auditLogService.log("CREATE_PREREQ", "Prerequisite", savedPrereq.getId().longValue(), null, savedPrereq.toString());
        
        return mapEntityToResponseDTO(savedPrereq);
    }

    @Transactional(readOnly = true)
    public List<PrerequisiteResponseDTO> getPrerequisitesForCourse(Integer courseId) {
        List<Prerequisite> prerequisites = prerequisiteRepository.findByCourseIdWithDetails(courseId);
        return prerequisites.stream()
                .map(this::mapEntityToResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deletePrerequisite(Integer id, Authentication authentication) {
        Prerequisite prerequisite = prerequisiteRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prerequisite not found with id: " + id));

        // Authorization Check: Must be admin or head of the *main course's* department
        checkUserAuthorityForCourse(authentication, prerequisite.getCourse().getDepartment().getId(), "delete");

        String oldData = prerequisite.toString();
        prerequisite.setDeleted(true);
        prerequisiteRepository.save(prerequisite);

        auditLogService.log("DELETE_PREREQ", "Prerequisite", id.longValue(), oldData, "DELETED");
    }

    // --- Helper Methods ---

    /**
     * Checks if the logged-in user is a SYSTEM_ADMIN or the DEPARTMENT_HEAD
     * for the given department ID.
     */
    private void checkUserAuthorityForCourse(Authentication authentication, Long departmentId, String action) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));

        if (isSystemAdmin) {
            return; // Admin can do anything
        }

        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

        if (isDeptHead && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return; // This is the correct Department Head
        }

        // If we get here, the user is not authorized
        throw new AccessDeniedException("You do not have permission to " + action + " prerequisites for this department.");
    }

    private PrerequisiteResponseDTO mapEntityToResponseDTO(Prerequisite entity) {
        PrerequisiteResponseDTO dto = new PrerequisiteResponseDTO();
        dto.setId(entity.getId());
        dto.setMinGrade(entity.getMinGrade());
        dto.setRequired(entity.isRequired());
        dto.setCourse(mapCourseToSummaryDTO(entity.getCourse()));
        dto.setPrerequisiteCourse(mapCourseToSummaryDTO(entity.getPrerequisiteCourse()));
        return dto;
    }

    private CourseSummaryDTO mapCourseToSummaryDTO(Course course) {
        if (course == null) return null;
        CourseSummaryDTO dto = new CourseSummaryDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCourseCode(course.getCourseCode());
        return dto;
    }
}