package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseRequestDTO;
import com.hopesapms.app.dto.CourseResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Course;
import com.hopesapms.app.model.Department;
import com.hopesapms.app.model.Instructor;
import com.hopesapms.app.model.Program;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.CourseRepository;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.InstructorRepository;
import com.hopesapms.app.repository.ProgramRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;
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

        Course course = Course.builder()
                .title(dto.getTitle())
                .courseCode(dto.getCourseCode())
                .credits(dto.getCredits())
                .description(dto.getDescription())
                .program(program)
                .department(department)
                .yearLevel(dto.getYearLevel())
                .prerequisite(dto.getPrerequisite())
                .build();

        Course savedCourse = courseRepository.save(course);
        auditLogService.log("CREATE_COURSE", "Course", savedCourse.getId().longValue(), null, savedCourse.toString());
        return mapEntityToDto(savedCourse);
    }

    @Transactional
    public List<CourseResponseDTO> bulkCreateCourses(List<CourseRequestDTO> dtoList, Authentication authentication) {
        User user = getUserFromAuth(authentication);
        if (user.getDepartment() == null) {
            throw new AccessDeniedException("You are not assigned to a department (faculty).");
        }
        Long userDepartmentId = user.getDepartment().getId();

        List<Course> coursesToSave = dtoList.stream().map(dto -> {
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

            return Course.builder()
                    .title(dto.getTitle())
                    .courseCode(dto.getCourseCode())
                    .credits(dto.getCredits())
                    .description(dto.getDescription())
                    .program(program)
                    .department(department)
                    .yearLevel(dto.getYearLevel())
                    .prerequisite(dto.getPrerequisite())
                    .build();
        }).collect(Collectors.toList());

        List<Course> savedCourses = courseRepository.saveAll(coursesToSave);

        return savedCourses.stream().map(course -> {
            auditLogService.log("CREATE_COURSE_BULK", "Course", course.getId().longValue(), null, course.toString());
            return mapEntityToDto(course);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<CourseResponseDTO> getCoursesByDepartment(Long departmentId, Pageable pageable) {
        Page<Course> coursePage = courseRepository.findByDepartment_IdAndIsDeletedFalse(departmentId, pageable);
        return coursePage.map(this::mapEntityToDto);
    }

    @Transactional(readOnly = true)
    public List<CourseResponseDTO> getCoursesByInstructorUsername(String username) {
        Instructor instructor = instructorRepository.findByUserUsername(username)
                .orElseThrow(() -> new RuntimeException("Instructor not found"));

        List<Course> courses = courseRepository.findByInstructor(instructor);

        return courses.stream()
                .map(this::mapEntityToDto)
                .toList();
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
        auditLogService.log("DELETE_COURSE", "Course", id.longValue(), oldData, "DELETED");
    }

    private User getUserFromAuth(Authentication authentication) {
        return userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
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

        throw new AccessDeniedException("You do not have permission to " + action + " this department.");
    }

    private CourseResponseDTO mapEntityToDto(Course course) {
        CourseResponseDTO dto = new CourseResponseDTO();
        dto.setId(course.getId());
        dto.setTitle(course.getTitle());
        dto.setCourseCode(course.getCourseCode());
        dto.setCredits(course.getCredits());
        dto.setDescription(course.getDescription());
        dto.setYearLevel(course.getYearLevel());
        dto.setPrerequisite(course.getPrerequisite());

        if (course.getProgram() != null) {
            dto.setProgramId(course.getProgram().getId());
            dto.setProgramName(course.getProgram().getName());
        }

        if (course.getDepartment() != null) {
            dto.setDepartmentId(course.getDepartment().getId());
            dto.setDepartmentName(course.getDepartment().getName());
        }

        dto.setCreatedAt(course.getCreatedAt());
        dto.setUpdatedAt(course.getUpdatedAt());
        return dto;
    }
}