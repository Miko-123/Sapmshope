package com.hopesapms.app.service;

import com.hopesapms.app.dto.EnrollmentRequestDTO;
import com.hopesapms.app.dto.EnrollmentResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*; // Import all models
import com.hopesapms.app.repository.*; // Import all repos
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public EnrollmentResponseDTO enrollInCourse(EnrollmentRequestDTO dto, Authentication authentication) {
        
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        boolean isStudent = user.getRoles().stream()
                .anyMatch(role -> "STUDENT".equals(role.getName()));
        if (!isStudent) {
            throw new AccessDeniedException("Only students can enroll in courses.");
        }

        Course course = courseRepository.findByIdAndIsDeletedFalse(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + dto.getCourseId()));

        Enrollment existing = enrollmentRepository
                .findByStudentIdAndCourseId(user.getId(), course.getId());

        boolean isAdd = false; 

        if (existing != null) {
            
            if ("ENROLLED".equals(existing.getStatus()) || "IN_PROGRESS".equals(existing.getStatus())) {
                throw new IllegalArgumentException("You are already actively enrolled in this course: " + course.getCourseCode());
            }
            
            if ("COMPLETED".equals(existing.getStatus()) && !"F".equals(existing.getFinalGrade())) {
                 throw new IllegalArgumentException("You have already passed this course: " + course.getCourseCode());
            }

            if ("COMPLETED".equals(existing.getStatus()) && "F".equals(existing.getFinalGrade())) {
                isAdd = true;
            }
        }
        
        Enrollment newEnrollment = Enrollment.builder()
                .student(user)
                .course(course)
                .enrollmentDate(LocalDate.now())
                .status("ENROLLED") 
                .isAddStudent(isAdd) 
                .build();
        
        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);

        auditLogService.log("ENROLL_COURSE", "Enrollment", savedEnrollment.getId().longValue(), null, "Student " + user.getId() + " enrolled in " + course.getId());
        
        return mapToResponseDTO(savedEnrollment);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponseDTO> getMyEnrollments(Authentication authentication, Pageable pageable) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Page<Enrollment> enrollmentPage = enrollmentRepository.findByStudentId(user.getId(), pageable);
        
        return enrollmentPage.map(this::mapToResponseDTO);
    }

    private EnrollmentResponseDTO mapToResponseDTO(Enrollment e) {
        EnrollmentResponseDTO dto = new EnrollmentResponseDTO();
        dto.setEnrollmentId(e.getId());
        dto.setCourseId(e.getCourse().getId());
        dto.setCourseCode(e.getCourse().getCourseCode());
        dto.setCourseTitle(e.getCourse().getTitle());
        dto.setEnrollmentDate(e.getEnrollmentDate());
        dto.setStatus(e.getStatus());
        dto.setFinalGrade(e.getFinalGrade());
        dto.setAddStudent(e.isAddStudent());
        return dto;
    }
}