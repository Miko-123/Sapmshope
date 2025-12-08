package com.hopesapms.app.service;

import com.hopesapms.app.dto.BulkCourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.ScheduleSlotDTO;
import com.hopesapms.app.dto.UpdateScheduleRequestDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.util.PeriodUtil;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.hopesapms.app.event.CourseOfferingCreatedEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
        private final RoomRepository roomRepository;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public CourseOfferingResponseDTO createCourseOffering(CourseOfferingRequestDTO dto,
                        Authentication authentication) {

                Course course = courseRepository.findById(dto.getCourseId())
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));
                checkUserAuthorityForDepartment(loggedInUser, course.getDepartment().getId());
                AcademicSemester semester = academicSemesterRepository.findById(dto.getAcademicSemesterId())
                                .orElseThrow(() -> new ResourceNotFoundException("AcademicSemester not found"));
                Instructor instructor = instructorRepository.findByUser_Id(dto.getInstructorId())
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

                eventPublisher.publishEvent(new CourseOfferingCreatedEvent(this, saved.getId()));

                return mapToResponseDTO(saved);
        }

        @Transactional
        public List<CourseOfferingResponseDTO> createBulkOfferings(BulkCourseOfferingRequestDTO dto,
                        Authentication authentication) {

                User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Course course = courseRepository.findById(dto.getCourseId())
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

                checkUserAuthorityForDepartment(loggedInUser, course.getDepartment().getId());

                AcademicSemester semester = academicSemesterRepository.findById(dto.getAcademicSemesterId())
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

                List<CourseOffering> savedOfferings = new java.util.ArrayList<>();

                for (BulkCourseOfferingRequestDTO.SectionInstructorPair pair : dto.getAssignments()) {

                        Instructor instructor = instructorRepository.findByUser_Id(pair.getInstructorId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Instructor not found for ID: " + pair.getInstructorId()));

                        Section section = sectionRepository.findById(pair.getSectionId())
                                        .orElseThrow(() -> new ResourceNotFoundException(
                                                        "Section not found for ID: " + pair.getSectionId()));

                        CourseOffering offering = CourseOffering.builder()
                                        .course(course)
                                        .academicSemester(semester)
                                        .contactHours(dto.getContactHours())
                                        .yearLevels(dto.getYearLevels())
                                        .status(dto.getStatus())
                                        .instructor(instructor)
                                        .section(section)
                                        .build();

                        savedOfferings.add(courseOfferingRepository.save(offering));
                }

                auditLogService.log("BULK_CREATE_OFFERING", "CourseOffering", 0L, null,
                                "Created " + savedOfferings.size() + " offerings for " + course.getCourseCode());

                return savedOfferings.stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Transactional
        public CourseOfferingResponseDTO updateCourseSchedule(Long offeringId, UpdateScheduleRequestDTO dto) {
                CourseOffering offering = courseOfferingRepository.findById(offeringId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Course Offering not found with id:" + offeringId));

                int maxHours = offering.getContactHours();
                int scheduledHours = 0;
                if (dto.getScheduleSlots() != null) {
                        for (ScheduleSlotDTO slot : dto.getScheduleSlots()) {
                                scheduledHours += PeriodUtil.calculateDuration(slot.getPeriods());
                        }
                }
                if (scheduledHours > maxHours) {
                        throw new IllegalArgumentException(
                                        "Total scheduled hours (" + scheduledHours
                                                        + ") exceed the contact hours limit (" + maxHours + ").");
                }

                if (dto.getScheduleSlots() != null && !dto.getScheduleSlots().isEmpty()) {
                        validateConflicts(offering, dto.getScheduleSlots());
                }

                String oldStatus = offering.getStatus();
                offering.setStatus(dto.getStatus());
                offering.getScheduleSlots().clear();

                if (dto.getScheduleSlots() != null) {
                        for (ScheduleSlotDTO slotDTO : dto.getScheduleSlots()) {
                                String normalizedPeriods = PeriodUtil.normalize(slotDTO.getPeriods());
                                offering.addScheduleSlot(slotDTO.getDay(), normalizedPeriods, slotDTO.getRoom());
                        }
                }

                CourseOffering saved = courseOfferingRepository.save(offering);
                auditLogService.log("UPDATE_SCHEDULE", "CourseOffering", saved.getId(), "Status: " + oldStatus,
                                "Status: " + saved.getStatus() + ", Slots" + saved.getScheduleSlots().size());

                return mapToResponseDTO(saved);
        }

        @Transactional(readOnly = true)
        public List<CourseOfferingResponseDTO> getOfferingsBySemester(Long semesterId) {

                String username = SecurityContextHolder.getContext().getAuthentication().getName();
                User currentUser = userRepository.findByEmailAndIsDeletedFalse(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                List<CourseOffering> offerings;
                boolean isDeptHead = currentUser.getRoles().stream()
                                .anyMatch(r -> r.getName().equals("DEPARTMENT_HEAD"));
                if (isDeptHead) {
                        if (currentUser.getDepartment() == null)
                                throw new IllegalStateException("Dept Head user has no Department!");
                        offerings = courseOfferingRepository.findBySemesterAndDepartment(semesterId,
                                        currentUser.getDepartment().getId());
                } else {
                        offerings = courseOfferingRepository.findByAcademicSemesterId(semesterId);
                }
                return offerings.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        }

        private void validateConflicts(CourseOffering currentOffering, List<ScheduleSlotDTO> newSlots) {
                Long semesterId = currentOffering.getAcademicSemester().getId();

                List<CourseOffering> activeOfferings = courseOfferingRepository
                                .findAllActiveInSemester(semesterId, "Active");

                activeOfferings.removeIf(o -> o.getId().equals(currentOffering.getId()));

                for (ScheduleSlotDTO newSlot : newSlots) {

                        Set<Integer> newTimeSlots = new HashSet<>(PeriodUtil.parsePeriods(newSlot.getPeriods()));

                        for (CourseOffering existing : activeOfferings) {
                                for (CourseSchedule existingSlot : existing.getScheduleSlots()) {

                                        if (!existingSlot.getDay().equalsIgnoreCase(newSlot.getDay())) {
                                                continue;
                                        }

                                        Set<Integer> existingTimeSlots = new HashSet<>(
                                                        PeriodUtil.parsePeriods(existingSlot.getPeriods()));

                                        boolean timeOverlap = !Collections.disjoint(newTimeSlots, existingTimeSlots);

                                        if (timeOverlap) {

                                                if (existingSlot.getRoom().equalsIgnoreCase(newSlot.getRoom())) {
                                                        throw new IllegalStateException(
                                                                        String.format("Room Conflict: %s is already booked on %s (Periods: %s) for %s.",
                                                                                        newSlot.getRoom(),
                                                                                        newSlot.getDay(),
                                                                                        existingSlot.getPeriods(),
                                                                                        existing.getCourse()
                                                                                                        .getCourseCode()));
                                                }

                                                if (existing.getInstructor().getId()
                                                                .equals(currentOffering.getInstructor().getId())) {
                                                        throw new IllegalStateException(
                                                                        String.format("Instructor Conflict: %s is already teaching %s on %s (Periods: %s).",
                                                                                        currentOffering.getInstructor()
                                                                                                        .getUser()
                                                                                                        .getFirstName(),
                                                                                        existing.getCourse()
                                                                                                        .getCourseCode(),
                                                                                        newSlot.getDay(),
                                                                                        existingSlot.getPeriods()));
                                                }

                                                if (existing.getSection().getId()
                                                                .equals(currentOffering.getSection().getId())) {
                                                        throw new IllegalStateException(
                                                                        String.format("Section Conflict: %s is already attending %s on %s (Periods: %s).",
                                                                                        currentOffering.getSection()
                                                                                                        .getName(),
                                                                                        existing.getCourse()
                                                                                                        .getCourseCode(),
                                                                                        newSlot.getDay(),
                                                                                        existingSlot.getPeriods()));
                                                }
                                        }
                                }
                        }
                }
        }

        @Transactional(readOnly = true)
        public List<Room> getAvailableRooms(Long semesterId, String day, String periods) {
                List<Room> allRooms = roomRepository.findByIsDeletedFalse();

                List<CourseOffering> activeOfferings = courseOfferingRepository.findAllActiveInSemester(semesterId,
                                "ACTIVE");
                Set<String> occupiedRoomNames = new HashSet<>();
                Set<Integer> requestedTimeSlots = new HashSet<>(PeriodUtil.parsePeriods(periods));

                for (CourseOffering offering : activeOfferings) {
                        for (CourseSchedule slot : offering.getScheduleSlots()) {

                                if (slot.getDay().equalsIgnoreCase(day)) {

                                        Set<Integer> existingTimeSlots = new HashSet<>(
                                                        PeriodUtil.parsePeriods(slot.getPeriods()));

                                        if (!Collections.disjoint(requestedTimeSlots, existingTimeSlots)) {
                                                occupiedRoomNames.add(slot.getRoom());
                                        }
                                }
                        }
                }

                return allRooms.stream()
                                .filter(room -> !occupiedRoomNames.contains(room.getName()))
                                .collect(Collectors.toList());
        }

        private CourseOfferingResponseDTO mapToResponseDTO(CourseOffering offering) {

                CourseOfferingResponseDTO dto = new CourseOfferingResponseDTO();
                dto.setId(offering.getId());
                dto.setStatus(offering.getStatus());
                dto.setCreatedAt(offering.getCreatedAt());
                List<ScheduleSlotDTO> scheduleSlots = offering.getScheduleSlots().stream()
                                .map(this::mapScheduleSlotToDTO).collect(Collectors.toList());
                dto.setScheduleSlots(scheduleSlots);
                if (offering.getCourse().getDepartment() != null) {
                        dto.setDepartmentName(offering.getCourse().getDepartment().getName());
                }
                dto.setCourseId(offering.getCourse().getId());
                dto.setCourseCode(offering.getCourse().getCourseCode());
                dto.setCourseTitle(offering.getCourse().getTitle());
                dto.setCreditHour(offering.getCourse().getCredits());
                dto.setContactHours(offering.getContactHours());
                dto.setYearLevels(offering.getYearLevels());
                dto.setSemesterId(offering.getAcademicSemester().getId());
                dto.setSemesterName(offering.getAcademicSemester().getName());
                dto.setSectionYearLevel(offering.getSection().getYearLevel());
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
                if (isDeptHead && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId))
                        return;
                boolean isProgramOfficer = user.getRoles().stream()
                                .anyMatch(role -> "PROGRAM_OFFICER".equals(role.getName()));
                if (isProgramOfficer)
                        return;
                throw new AccessDeniedException(
                                "You do not have permission to create an offering for this department.");
        }

        private ScheduleSlotDTO mapScheduleSlotToDTO(CourseSchedule scheduleSlot) {
                ScheduleSlotDTO dto = new ScheduleSlotDTO();
                dto.setDay(scheduleSlot.getDay());
                dto.setPeriods(scheduleSlot.getPeriods());
                dto.setRoom(scheduleSlot.getRoom());
                return dto;
        }
}