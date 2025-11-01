package com.hopesapms.app.service;

import com.hopesapms.app.dto.ScheduleDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.ClassSession;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.ClassSessionRepository;
import com.hopesapms.app.repository.EnrollmentRepository;
import com.hopesapms.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassSessionRepository classSessionRepository;

    @Transactional(readOnly = true)
    public List<ScheduleDTO> getMySchedule(Authentication authentication) {
        
        // 1. Get Logged-in User
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        // 2. Get all their *active* enrollments
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(user.getId(), Pageable.unpaged())
                .stream()
                .filter(e -> "ENROLLED".equals(e.getStatus()) || "IN_PROGRESS".equals(e.getStatus()))
                .collect(Collectors.toList());

        // 3. Extract the Course IDs
        List<Integer> courseIds = enrollments.stream()
                .map(e -> e.getCourse().getId())
                .distinct()
                .collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            return List.of(); // Student is not enrolled in anything
        }

        // 4. Find all future class sessions for those courses
        List<ClassSession> sessions = classSessionRepository.findByCourseIdInAndDateAfter(courseIds, LocalDate.now());

        // 5. Map to DTO
        return sessions.stream()
                .map(this::mapToScheduleDTO)
                .collect(Collectors.toList());
    }
    
    private ScheduleDTO mapToScheduleDTO(ClassSession cs) {
        ScheduleDTO dto = new ScheduleDTO();
        dto.setClassSessionId(cs.getId());
        dto.setCourseCode(cs.getCourse().getCourseCode());
        dto.setCourseTitle(cs.getCourse().getTitle());
        dto.setSessionDate(cs.getSessionDate());
        dto.setSessionTime(cs.getSessionTime());
        dto.setLocation(cs.getLocation());
        dto.setInstructorName(cs.getScheduledInstructor().getFirstName() + " " + cs.getScheduledInstructor().getLastName());
        return dto;
    }
}