package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseRequestDTO;
import com.hopesapms.app.dto.CourseResponseDTO;
import com.hopesapms.app.dto.PrerequisiteRequestDTO;
import com.hopesapms.app.dto.PrerequisiteResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.CourseCategory;
import com.hopesapms.app.model.Department;
import com.hopesapms.app.model.Prerequisite;
import com.hopesapms.app.model.Program;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.CourseRepository;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.PrerequisiteRepository;
import com.hopesapms.app.repository.ProgramRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final PrerequisiteRepository prerequisiteRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public CourseResponseDTO createCourse(CourseRequestDTO dto, Authentication authentication) {

        Program program = programRepository.findByIdAndIsDeletedFalse(dto.getProgramId())
                .orElseThrow(() -> new ResourceNotFoundException("Program not found with id: " + dto.getProgramId()));

        Department department = program.getDepartment();
        if (department == null) {
            throw new IllegalStateException("Program " + program.getId() + " is not linked to a department.");
        }

        checkUserAuthorityForDepartment(authentication, department.getId(), "create course in");

        if (courseRepository.findByCourseCodeAndIsDeletedFalse(dto.getCourseCode()).isPresent()) {
            throw new EntityExistsException("Course code '" + dto.getCourseCode() + "' already exists.");
        }

        CourseCategory category;
        try {
            category = CourseCategory.valueOf(dto.getCategory().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid course category: " + dto.getCategory());
        }

        Course course = Course.builder()
                .title(dto.getTitle())
                .courseCode(dto.getCourseCode())
                .credits(dto.getCredits())
                .description(dto.getDescription())
                .program(program)
                .department(department)
                .yearLevel(dto.getYearLevel())
                .category(category)
                .build();

        Course savedCourse = courseRepository.save(course);

        if (dto.getPrerequisites() != null && !dto.getPrerequisites().isEmpty()) {
            savePrerequisitesForCourse(savedCourse, dto.getPrerequisites());
        }

        auditLogService.log("CREATE_COURSE", "Course", savedCourse.getId().longValue(), null, savedCourse.toString());
        return mapEntityToDto(savedCourse);
    }

    @Transactional
    public List<CourseResponseDTO> bulkCreateCourses(List<CourseRequestDTO> dtoList, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (user.getDepartment() == null)
            throw new AccessDeniedException("You are not assigned to a department.");
        Long userDepartmentId = user.getDepartment().getId();

        List<Course> coursesToSave = new ArrayList<>();
        Map<CourseRequestDTO, Course> dtoToCourse = new HashMap<>();

        for (CourseRequestDTO dto : dtoList) {
            Program program = programRepository.findByIdAndIsDeletedFalse(dto.getProgramId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Program not found with id: " + dto.getProgramId()));

            Department department = program.getDepartment();
            if (!department.getId().equals(userDepartmentId)) {
                throw new AccessDeniedException("You cannot add course to program '" + program.getName()
                        + "' as it is not in your department.");
            }

            if (courseRepository.findByCourseCodeAndIsDeletedFalse(dto.getCourseCode()).isPresent()) {
                throw new EntityExistsException("Course code '" + dto.getCourseCode() + "' already exists.");
            }

            CourseCategory category;
            try {
                category = CourseCategory.valueOf(dto.getCategory().toUpperCase());
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Invalid category: " + dto.getCategory() + " for course " + dto.getCourseCode());
            }

            Course c = Course.builder()
                    .title(dto.getTitle())
                    .courseCode(dto.getCourseCode())
                    .credits(dto.getCredits())
                    .description(dto.getDescription())
                    .program(program)
                    .department(department)
                    .yearLevel(dto.getYearLevel())
                    .category(category)
                    .build();

            coursesToSave.add(c);
            dtoToCourse.put(dto, c);
        }

        List<Course> savedCourses = courseRepository.saveAll(coursesToSave);

        Map<String, Course> codeToSaved = savedCourses.stream()
                .collect(Collectors.toMap(Course::getCourseCode, c -> c));

        for (CourseRequestDTO dto : dtoList) {
            Course saved = codeToSaved.get(dto.getCourseCode());
            if (dto.getPrerequisites() != null && !dto.getPrerequisites().isEmpty()) {
                savePrerequisitesForCourse(saved, dto.getPrerequisites());
            }
            auditLogService.log("CREATE_COURSE_BULK", "Course", saved.getId().longValue(), null, saved.toString());
        }

        return savedCourses.stream().map(this::mapEntityToDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CourseResponseDTO> getCoursesByDepartment(Long departmentId, Pageable pageable) {
        Page<Course> coursePage = courseRepository.findByDepartment_IdAndIsDeletedFalse(departmentId, pageable);
        return coursePage.map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public CourseResponseDTO getCourseById(Integer id) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));
        return mapEntityToDto(course);
    }

    @Transactional
    public void deleteCourse(Integer id, Authentication authentication) {
        Course course = courseRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        checkUserAuthorityForDepartment(authentication, course.getDepartment().getId(), "delete course from");

        String oldData = course.toString();
        course.setDeleted(true);
        courseRepository.save(course);

        List<Prerequisite> prereqList = prerequisiteRepository.findByCourseIdWithDetails(course.getId());
        for (Prerequisite p : prereqList) {
            p.setDeleted(true);
            prerequisiteRepository.save(p);
        }

        auditLogService.log("DELETE_COURSE", "Course", id.longValue(), oldData, "DELETED");
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));
    }

    private User checkUserAuthorityForDepartment(Authentication authentication, Long departmentId, String action) {
        User user = getUserFromAuth(authentication);

        boolean isSystemAdmin = user.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
        if (isSystemAdmin)
            return user;

        boolean isDeptHead = user.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

        if (isDeptHead && user.getDepartment() != null && user.getDepartment().getId().equals(departmentId)) {
            return user;
        }

        boolean isProgramOfficer = user.getRoles().stream()
                .anyMatch(role -> "PROGRAM_OFFICER".equals(role.getName()));
        if (isProgramOfficer)
            return user;

        throw new AccessDeniedException("You do not have permission to " + action + " this department.");
    }

    private void savePrerequisitesForCourse(Course course, List<PrerequisiteRequestDTO> prerequisites) {

        Course managedCourse = courseRepository.findById(course.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found during prerequisite save"));

        List<Prerequisite> existing = prerequisiteRepository.findByCourseIdWithDetails(managedCourse.getId());
        for (Prerequisite p : existing) {
            p.setDeleted(true);
            prerequisiteRepository.save(p);
        }

        for (PrerequisiteRequestDTO prDto : prerequisites) {
            Integer prereqCourseId = prDto.getPrerequisiteCourseId();
            boolean isRequired = prDto.getIsRequired() == null ? true : prDto.getIsRequired();

            Course prereqCourse = courseRepository.findByIdAndIsDeletedFalse(prereqCourseId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Prerequisite course not found with id: " + prereqCourseId));

            if (Objects.equals(prereqCourse.getId(), managedCourse.getId())) {
                throw new IllegalArgumentException(
                        "Course cannot be a prerequisite of itself: " + managedCourse.getCourseCode());
            }

            if (detectCycle(prereqCourse, managedCourse)) {
                throw new IllegalArgumentException("Adding prerequisite would create a cycle: "
                        + prereqCourse.getCourseCode() + " -> ... -> " + managedCourse.getCourseCode());
            }

            boolean exists = prerequisiteRepository.existsByCourse_IdAndPrerequisiteCourse_IdAndIsDeletedFalse(
                    managedCourse.getId(), prereqCourse.getId());
            if (exists)
                continue;

            Prerequisite p = Prerequisite.builder()
                    .course(managedCourse)
                    .prerequisiteCourse(prereqCourse)
                    .isRequired(isRequired)
                    .build();

            prerequisiteRepository.save(p);
        }
    }

    private boolean detectCycle(Course start, Course targetCourse) {

        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(start.getId());

        while (!stack.isEmpty()) {
            Integer currentId = stack.pop();
            if (!visited.add(currentId))
                continue;

            if (currentId.equals(targetCourse.getId())) {
                return true;
            }

            List<Prerequisite> next = prerequisiteRepository.findByCourseIdWithDetails(currentId);
            for (Prerequisite p : next) {
                if (!p.isDeleted() && p.getPrerequisiteCourse() != null) {
                    stack.push(p.getPrerequisiteCourse().getId());
                }
            }
        }

        return false;
    }

    private CourseResponseDTO mapEntityToDto(Course course) {
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCourseCode(course.getCourseCode());
        dto.setCredits(course.getCredits());
        dto.setDescription(course.getDescription());
        dto.setYearLevel(course.getYearLevel());

        if (course.getCategory() != null) {
            dto.setCategory(course.getCategory().name());
        }

        if (course.getProgram() != null) {
            dto.setProgramId(course.getProgram().getId());
            dto.setProgramName(course.getProgram().getName());
        }

        if (course.getDepartment() != null) {
            dto.setDepartmentId(course.getDepartment().getId());
            dto.setDepartmentName(course.getDepartment().getName());
        }

        // fetch prerequisites and map them
        List<Prerequisite> prereqList = prerequisiteRepository.findByCourseIdWithDetails(course.getId());
        List<PrerequisiteResponseDTO> prereqDtos = prereqList.stream()
                .filter(p -> !p.isDeleted())
                .map(p -> {
                    PrerequisiteResponseDTO pr = new PrerequisiteResponseDTO();
                    pr.setId(p.getId());
                    pr.setPrerequisiteCourseId(p.getPrerequisiteCourse().getId());
                    pr.setPrerequisiteCourseCode(p.getPrerequisiteCourse().getCourseCode());
                    pr.setPrerequisiteCourseTitle(p.getPrerequisiteCourse().getTitle());
                    pr.setRequired(p.isRequired());
                    return pr;
                })
                .collect(Collectors.toList());

        dto.setPrerequisites(prereqDtos);
        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());
        return dto;
    }
}