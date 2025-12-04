package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.CourseOfferingSearchResultDTO;
import com.hopesapms.app.dto.ScheduleSlotDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException; // <-- Import this
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseOfferingService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final CourseRepository courseRepository;
    private final AcademicSemesterRepository academicSemesterRepository;
    private final InstructorRepository instructorRepository;
    private final SectionRepository sectionRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @Transactional
    public CourseOfferingResponseDTO createCourseOffering(CourseOfferingRequestDTO dto, Authentication authentication) {

        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        checkUserAuthorityForDepartment(loggedInUser, course.getDepartment().getId());

        AcademicSemester semester = academicSemesterRepository.findById(dto.getAcademicSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found"));

        Instructor instructor = instructorRepository.findByUserId(dto.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));

        Section section = sectionRepository.findById(dto.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        CourseOffering offering = CourseOffering.builder()
                .course(course)
                .academicSemester(semester)
                .instructor(instructor)
                .section(section)
                .yearLevels(dto.getYearLevels())
                .contactHours(dto.getContactHours())
                .status(dto.getStatus())

                .build();

        if (dto.getScheduleSlots() != null) {
            for (ScheduleSlotDTO slotDTO : dto.getScheduleSlots()) {
                offering.addScheduleSlot(slotDTO.getDay(), slotDTO.getPeriods(), slotDTO.getRoom());
            }
        }

        CourseOffering saved = courseOfferingRepository.save(offering);
        auditLogService.log("CREATE_COURSE_OFFERING", "CourseOffering", saved.getId(), null, saved.toString());

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingResponseDTO> getOfferingsBySemester(Long semesterId) {
        List<CourseOffering> offerings = courseOfferingRepository.findByAcademicSemester_IdAndIsDeletedFalse(semesterId);
        return offerings.stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
    }

    private CourseOfferingResponseDTO mapToResponseDTO(CourseOffering offering) {
        CourseOfferingResponseDTO dto = new CourseOfferingResponseDTO();
        dto.setId(offering.getId());
        dto.setStatus(offering.getStatus());
        dto.setCreatedAt(offering.getCreatedAt());

        List<ScheduleSlotDTO> scheduleSlots = offering.getScheduleSlots().stream()
                .map(this::mapScheduleSlotToDTO)
                .collect(Collectors.toList());
        dto.setScheduleSlots(scheduleSlots);

        dto.setCourseId(offering.getCourse().getId());
        dto.setCourseCode(offering.getCourse().getCourseCode());
        dto.setCourseTitle(offering.getCourse().getTitle());
        dto.setCreditHour(offering.getCourse().getCredits());
        dto.setContactHours(offering.getContactHours());
        dto.setYearLevels(offering.getYearLevels());

        dto.setSemesterId(offering.getAcademicSemester().getId());
        dto.setSemesterName(offering.getAcademicSemester().getName());

        dto.setInstructorId(offering.getInstructor().getId());
        dto.setInstructorName(offering.getInstructor().getUser().getFirstName() + " "
                + offering.getInstructor().getUser().getMiddleName());

        dto.setSectionId(offering.getSection().getId());
        dto.setSectionName(offering.getSection().getName());

        return dto;
    }

    private void checkUserAuthorityForDepartment(User user, Long departmentId) {
        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin)
            return;

        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

        if (isDeptHead && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return;
        }

        boolean isProgramOfficer = user.getRoles().stream()
                .anyMatch(role -> "PROGRAM_OFFICER".equals(role.getName()));
        if (isProgramOfficer)
            return;

        throw new AccessDeniedException("You do not have permission to create an offering for this department.");
    }

    private ScheduleSlotDTO mapScheduleSlotToDTO(CourseSchedule scheduleSlot) {
        ScheduleSlotDTO dto = new ScheduleSlotDTO();
        dto.setDay(scheduleSlot.getDay());
        dto.setPeriods(scheduleSlot.getPeriods());
        dto.setRoom(scheduleSlot.getRoom());
        return dto;
    }
}