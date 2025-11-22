package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.CourseOfferingSearchResultDTO;
import com.hopesapms.app.dto.ScheduleSlotDTO; 
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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

    @Transactional
    public CourseOfferingResponseDTO createCourseOffering(CourseOfferingRequestDTO dto) {
        
        Course course = courseRepository.findById(dto.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
            
        AcademicSemester semester = academicSemesterRepository.findById(dto.getAcademicSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found"));

        Instructor instructor = instructorRepository.findById(dto.getInstructorId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
            
        Section section = sectionRepository.findById(dto.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
        CourseOffering offering = CourseOffering.builder()
                .course(course)
                .academicSemester(semester)
                .instructor(instructor)
                .section(section)
                .status(dto.getStatus())
                
                .build();

        try {
            Method m = dto.getClass().getMethod("getScheduleSlots");
            @SuppressWarnings("unchecked")
            List<ScheduleSlotDTO> slotList = (List<ScheduleSlotDTO>) m.invoke(dto);
            if (slotList != null) {
                for (ScheduleSlotDTO slotDTO : slotList) {
                    offering.addScheduleSlot(slotDTO.getDay(), slotDTO.getPeriods(), slotDTO.getRoom());
                }
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        }

        CourseOffering saved = courseOfferingRepository.save(offering);
        auditLogService.log("CREATE_COURSE_OFFERING", "CourseOffering", saved.getId(), null, saved.toString());

        return mapToResponseDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingSearchResultDTO> getOfferingsBySemester(Long semesterId) { // <-- MUST BE Long
        
        List<CourseOffering> offerings = courseOfferingRepository
            .findByAcademicSemester_IdAndIsDeletedFalse(semesterId); // <-- Pass the Long
            
        return offerings.stream()
            .map(CourseOfferingSearchResultDTO::new)
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
        dto.setCredits(offering.getCourse().getCredits());
        dto.setYearLevel(offering.getCourse().getYearLevel());

        dto.setSemesterId(offering.getAcademicSemester().getId());
        dto.setSemesterName(offering.getAcademicSemester().getName());

        dto.setInstructorId(offering.getInstructor().getId());
        dto.setInstructorName(offering.getInstructor().getUser().getFirstName() + " " + offering.getInstructor().getUser().getLastName());

        dto.setSectionId(offering.getSection().getId());
        dto.setSectionName(offering.getSection().getName());

        return dto;
    }

    private ScheduleSlotDTO mapScheduleSlotToDTO(CourseSchedule scheduleSlot) {
        ScheduleSlotDTO dto = new ScheduleSlotDTO();
        dto.setDay(scheduleSlot.getDay());
        dto.setPeriods(scheduleSlot.getPeriods());
        dto.setRoom(scheduleSlot.getRoom());
        return dto;
    }
}