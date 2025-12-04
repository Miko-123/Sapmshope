package com.hopesapms.app.service;

import com.hopesapms.app.dto.ClassSessionRequestDTO;
import com.hopesapms.app.dto.ClassSessionResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.ClassSession;
import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.ClassSessionRepository;
import com.hopesapms.app.repository.CourseRepository;
import com.hopesapms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Replaces @Autowired
public class ClassSessionService {

    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public ClassSessionResponseDTO createClassSession(ClassSessionRequestDTO dto, Authentication authentication) {
        
        Course course = courseRepository.findByIdAndIsDeletedFalse(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
        User instructor = userRepository.findById(dto.getScheduledInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        checkUserAuthorityForCourse(authentication, course.getDepartment().getId(), "create session for");

        ClassSession session = ClassSession.builder()
                .course(course)
                .sessionDate(dto.getSessionDate())
                .sessionTime(dto.getSessionTime())
                .location(dto.getLocation())
                .scheduledInstructor(instructor)
                .build();

        ClassSession saved = classSessionRepository.save(session);
        auditLogService.log("CREATE_SESSION", "ClassSession", saved.getId().longValue(), null, saved.toString());
        return mapToDTO(saved);
    }
    
    @Transactional(readOnly = true)
    public Page<ClassSessionResponseDTO> getClassSessionsByCourseId(Integer courseId, Pageable pageable) {
        return classSessionRepository.findByCourseId(courseId, pageable)
                .map(this::mapToDTO);
    }

    @Transactional
    public void deleteClassSession(Integer id, Authentication authentication) {
        ClassSession session = classSessionRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("ClassSession not found"));

        checkUserAuthorityForCourse(authentication, session.getCourse().getDepartment().getId(), "delete session for");

        String oldData = session.toString();
        session.setDeleted(true);
        classSessionRepository.save(session);
        auditLogService.log("DELETE_SESSION", "ClassSession", id.longValue(), oldData, "DELETED");
    }
    
    private void checkUserAuthorityForCourse(Authentication authentication, Long departmentId, String action) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

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
    
    private ClassSessionResponseDTO mapToDTO(ClassSession session) {
        ClassSessionResponseDTO dto = new ClassSessionResponseDTO();
        dto.setId(session.getId());
        dto.setCourseId(session.getCourse().getId());
        dto.setCourseName(session.getCourse().getTitle());
        dto.setSessionDate(session.getSessionDate());
        dto.setSessionTime(session.getSessionTime());
        dto.setLocation(session.getLocation());
        dto.setScheduledInstructorId(session.getScheduledInstructor().getId());
        dto.setScheduledInstructorName(session.getScheduledInstructor().getFirstName() + " " + session.getScheduledInstructor().getLastName());
        return dto;
    }
}