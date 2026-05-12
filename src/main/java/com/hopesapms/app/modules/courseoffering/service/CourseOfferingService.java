package com.hopesapms.app.modules.courseoffering.service;

import com.hopesapms.app.modules.courseoffering.dto.BulkCourseOfferingRequestDTO;
import com.hopesapms.app.modules.courseoffering.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.modules.courseoffering.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.modules.schedule.dto.ScheduleSlotDTO;
import com.hopesapms.app.modules.schedule.dto.UpdateScheduleRequestDTO;
import com.hopesapms.app.common.exception.ResourceNotFoundException;
import com.hopesapms.app.modules.academicsemester.model.AcademicSemester;
import com.hopesapms.app.modules.course.model.Course;
import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import com.hopesapms.app.modules.schedule.model.CourseSchedule;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.room.model.Room;
import com.hopesapms.app.modules.section.model.Section;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.common.util.PeriodUtil;
import com.hopesapms.app.modules.academicsemester.repository.AcademicSemesterRepository;
import com.hopesapms.app.modules.courseoffering.repository.CourseOfferingRepository;
import com.hopesapms.app.modules.course.repository.CourseRepository;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.room.repository.RoomRepository;
import com.hopesapms.app.modules.section.repository.SectionRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.academicsemester.service.AcademicSemesterService;
import com.hopesapms.app.modules.auditlog.service.AuditLogService;
import com.hopesapms.app.modules.enrollment.service.EnrollmentService;

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
        @Lazy
        private final EnrollmentService enrollmentService;
        private final AcademicSemesterService semesterService;

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
                Instructor instructor = instructorRepository.findByUser_Id(dto.getInstructorId().intValue())
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

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                try {
                                        enrollmentService.enrollSectionInNewOffering(saved.getId());
                                } catch (Exception e) {
                                        System.err.println("Auto-enroll failed: " + e.getMessage());
                                }
                        }
                });

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

                        Instructor instructor = instructorRepository.findByUser_Id(pair.getInstructorId().intValue())
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

                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                                for (CourseOffering saved : savedOfferings) {
                                        try {
                                                System.out.println(">>> Auto-Syncing Enrollment for Offering ID: "
                                                                + saved.getId());
                                                enrollmentService.enrollSectionInNewOffering(saved.getId());
                                        } catch (Exception e) {
                                                System.err.println("Auto-enroll failed for offering " + saved.getId()
                                                                + ": " + e.getMessage());
                                        }
                                }
                        }
                });

                return savedOfferings.stream()
                                .map(this::mapToResponseDTO)
                                .collect(Collectors.toList());
        }

        @Transactional
        public List<CourseOfferingResponseDTO> bulkUpdateOfferings(BulkCourseOfferingRequestDTO dto,
                        Authentication authentication) {

                User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                Course course = courseRepository.findById(dto.getCourseId())
                                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));
                checkUserAuthorityForDepartment(loggedInUser, course.getDepartment().getId());

                semesterService.validateSemesterEditable(dto.getAcademicSemesterId());

                AcademicSemester semester = academicSemesterRepository.findById(dto.getAcademicSemesterId())
                                .orElseThrow(() -> new ResourceNotFoundException("Semester not found"));

                List<CourseOffering> existingOfferings = courseOfferingRepository
                                .findByAcademicSemester_IdAndCourse_IdAndIsDeletedFalse(
                                                semester.getId().longValue(), course.getId());

                Map<Integer, CourseOffering> existingMap = existingOfferings.stream()
                                .collect(Collectors.toMap(o -> o.getSection().getId(), o -> o));

                List<CourseOffering> finalSavedList = new ArrayList<>();
                Set<Integer> processedSectionIds = new HashSet<>();

                for (BulkCourseOfferingRequestDTO.SectionInstructorPair pair : dto.getAssignments()) {
                        processedSectionIds.add(pair.getSectionId());

                        Instructor instructor = null;
                        if (pair.getInstructorId() != null && pair.getInstructorId() > 0) {
                                instructor = instructorRepository.findByUser_Id(pair.getInstructorId().intValue())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Instructor not found"));
                        }

                        if (existingMap.containsKey(pair.getSectionId())) {

                                CourseOffering existing = existingMap.get(pair.getSectionId());
                                boolean changed = !existing.getInstructor().getId().equals(instructor.getId());

                                if (changed) {
                                        existing.setInstructor(instructor);

                                        existing.setContactHours(dto.getContactHours());
                                        existing = courseOfferingRepository.save(existing);
                                }
                                finalSavedList.add(existing);
                        } else {

                                Section section = sectionRepository.findById(pair.getSectionId())
                                                .orElseThrow(() -> new ResourceNotFoundException("Section not found"));

                                CourseOffering newOffering = CourseOffering.builder()
                                                .course(course)
                                                .academicSemester(semester)
                                                .section(section)
                                                .instructor(instructor)
                                                .contactHours(dto.getContactHours())
                                                .status("PLANNED")
                                                .yearLevels(dto.getYearLevels())
                                                .build();

                                finalSavedList.add(courseOfferingRepository.save(newOffering));
                        }
                }

                for (CourseOffering existing : existingOfferings) {
                        if (!processedSectionIds.contains(existing.getSection().getId())) {

                                existing.setDeleted(true);
                                courseOfferingRepository.save(existing);
                        }
                }

                auditLogService.log("BULK_UPDATE_OFFERING", "CourseOffering", 0L,
                                "Previous count: " + existingOfferings.size(),
                                "New count: " + finalSavedList.size());

                return finalSavedList.stream().map(this::mapToResponseDTO).collect(Collectors.toList());
        }

        @Transactional
        public CourseOfferingResponseDTO updateCourseSchedule(Long offeringId, UpdateScheduleRequestDTO dto) {
                CourseOffering offering = courseOfferingRepository.findById(offeringId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Course Offering not found with id:" + offeringId));

                semesterService.validateSemesterEditable(offering.getAcademicSemester().getId());

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

        @Transactional
        public int copyDepartmentOfferings(Long sourceSemesterId, Long targetSemesterId, String username) {

                User deptHead = userRepository.findByEmailAndIsDeletedFalse(username)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                if (deptHead.getDepartment() == null) {
                        throw new IllegalStateException("You are not assigned to a department.");
                }
                Long deptId = deptHead.getDepartment().getId();

                AcademicSemester target = academicSemesterRepository.findById(targetSemesterId)
                                .orElseThrow(() -> new ResourceNotFoundException("Target semester not found"));

                if (target.isArchived()) {
                        throw new IllegalStateException("Cannot copy into an ARCHIVED semester.");
                }

                List<CourseOffering> sourceOfferings = courseOfferingRepository
                                .findBySemesterAndDepartment(sourceSemesterId, deptId);

                int count = 0;

                for (CourseOffering original : sourceOfferings) {
                        boolean exists = courseOfferingRepository
                                        .existsByAcademicSemester_IdAndCourse_IdAndSection_IdAndIsDeletedFalse(
                                                        targetSemesterId,
                                                        original.getCourse().getId(),
                                                        original.getSection().getId());

                        if (!exists) {

                                CourseOffering newOffering = CourseOffering.builder()
                                                .academicSemester(target)
                                                .course(original.getCourse())
                                                .section(original.getSection())
                                                .instructor(original.getInstructor())
                                                .yearLevels(original.getYearLevels())
                                                .contactHours(original.getContactHours())
                                                .status("PLANNED")
                                                .isDeleted(false)
                                                .build();

                                CourseOffering saved = courseOfferingRepository.save(newOffering);

                                if (original.getScheduleSlots() != null && !original.getScheduleSlots().isEmpty()) {
                                        for (CourseSchedule oldSlot : original.getScheduleSlots()) {
                                                saved.addScheduleSlot(
                                                                oldSlot.getDay(),
                                                                oldSlot.getPeriods(),
                                                                oldSlot.getRoom());
                                        }
                                        courseOfferingRepository.save(saved);
                                }
                                count++;
                        }
                }
                return count;
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

        @Transactional
        public CourseOfferingResponseDTO updateCourseOffering(Long id, CourseOfferingRequestDTO dto,
                        Authentication authentication) {

                CourseOffering offering = courseOfferingRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Course Offering not found with id: " + id));

                User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                checkUserAuthorityForDepartment(loggedInUser, offering.getCourse().getDepartment().getId());

                String oldData = offering.toString();

                if (dto.getInstructorId() != null) {
                        Instructor newInstructor = instructorRepository.findByUser_Id(dto.getInstructorId().intValue())
                                        .orElseThrow(() -> new ResourceNotFoundException("Instructor not found"));
                        offering.setInstructor(newInstructor);
                } else {
                        offering.setInstructor(null);
                }

                if (dto.getSectionId() != null) {
                        Section newSection = sectionRepository.findById(dto.getSectionId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Section not found"));
                        offering.setSection(newSection);
                }

                if (dto.getStatus() != null)
                        offering.setStatus(dto.getStatus());
                if (dto.getContactHours() != null)
                        offering.setContactHours(dto.getContactHours());
                if (dto.getYearLevels() != null)
                        offering.setYearLevels(dto.getYearLevels());

                if (dto.getScheduleSlots() != null) {
                        offering.getScheduleSlots().clear();
                        for (ScheduleSlotDTO slotDTO : dto.getScheduleSlots()) {
                                offering.addScheduleSlot(slotDTO.getDay(), slotDTO.getPeriods(), slotDTO.getRoom());
                        }
                }

                CourseOffering saved = courseOfferingRepository.save(offering);
                auditLogService.log("UPDATE_COURSE_OFFERING", "CourseOffering", saved.getId(), oldData,
                                saved.toString());

                return mapToResponseDTO(saved);
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
                dto.setSectionId(offering.getSection().getId());
                dto.setSectionName(offering.getSection().getName());

                if (offering.getInstructor() != null) {
                        dto.setInstructorId(offering.getInstructor().getId());
                        if (offering.getInstructor().getUser() != null) {
                                dto.setInstructorName(offering.getInstructor().getUser().getFirstName() + " "
                                                + offering.getInstructor().getUser().getLastName() + " "
                                                + offering.getInstructor().getUser().getMiddleName());
                        } else {
                                dto.setInstructorName("Unknown Instructor");
                        }
                } else {
                        dto.setInstructorId(null);
                        dto.setInstructorName("TBD");
                }

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