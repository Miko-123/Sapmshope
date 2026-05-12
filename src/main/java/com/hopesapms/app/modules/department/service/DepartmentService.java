package com.hopesapms.app.modules.department.service;

import com.hopesapms.app.modules.courseoffering.dto.AssignStaffRequestDTO;
import com.hopesapms.app.modules.auditlog.dto.AuditLogResponse;
import com.hopesapms.app.modules.department.dto.CreateDepartmentRequest;
import com.hopesapms.app.modules.department.dto.DepartmentResponseDTO;
import com.hopesapms.app.modules.department.dto.DeptCourseDTO;
import com.hopesapms.app.modules.department.dto.DpHdDashboardStatsDTO;
import com.hopesapms.app.modules.department.dto.DepartmentSemesterGpaRawDto;
import com.hopesapms.app.modules.department.dto.UpdateDepartmentDetailsRequest;
import com.hopesapms.app.modules.user.dto.UserResponseDTO;
import com.hopesapms.app.common.exception.ResourceNotFoundException;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.auditlog.repository.AuditLogRepository;
import com.hopesapms.app.modules.courseoffering.repository.CourseOfferingRepository;
import com.hopesapms.app.modules.department.repository.DepartmentRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hopesapms.app.modules.auditlog.model.AuditLog;
import com.hopesapms.app.modules.auditlog.dto.AuditLogResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.auditlog.service.AuditLogService;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final StudentRepository studentRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final InstructorRepository instructorRepository;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;
    @Transactional
    public DepartmentResponseDTO createDepartment(CreateDepartmentRequest dto) {
        if (departmentRepository.existsByNameAndIsDeletedFalse(dto.getName())) {
            throw new EntityExistsException("A department with name '" + dto.getName() + "' already exists.");
        }
        Department department = new Department();
        department.setName(dto.getName());
        Department savedDept = departmentRepository.save(department);
        auditLogService.log("CREATE_DEPARTMENT", "Department", savedDept.getId(), null, savedDept.toString());
        return mapEntityToDto(savedDept);
    }

    @Transactional
    public DepartmentResponseDTO getMyDepartment(Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("Logged in user is not found"));

        if (user.getDepartment() == null) {
            throw new ResourceNotFoundException("You are not assigned to any department");
        }

        return mapEntityToDto(user.getDepartment());
    }

    @Transactional
    public DepartmentResponseDTO updateMyDepartmentDetails(UpdateDepartmentDetailsRequest dto,
            Authentication authentication) {
        User deptHead = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Department department = deptHead.getDepartment();
        if (department == null) {
            throw new AccessDeniedException("You are not assigned to a department.");
        }

        Optional<Department> existingByCode = departmentRepository.findByCodeAndIsDeletedFalse(dto.getCode());
        if (existingByCode.isPresent() && !existingByCode.get().getId().equals(department.getId())) {
            throw new EntityExistsException("A department with code '" + dto.getCode() + "' already exists.");
        }

        String oldData = department.toString();
        department.setCode(dto.getCode());
        department.setContactEmail(dto.getContactEmail());
        department.setContactPhone(dto.getContactPhone());
        department.setOfficeLocation(dto.getOfficeLocation());
        department.setDescription(dto.getDescription());
        Department updatedDept = departmentRepository.save(department);

        auditLogService.log("UPDATE_DEPT_DETAILS", "Department", updatedDept.getId(), oldData, updatedDept.toString());
        return mapEntityToDto(updatedDept);
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponseDTO> getAllDepartments() {
        return departmentRepository.findByIsDeletedFalse()
                .stream()
                .map(this::mapEntityToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeptCourseDTO> getCoursesForDeptHead(Authentication authentication) {

        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getDepartment() == null) {
            throw new IllegalStateException("You are not assigned to any department.");
        }

        Long deptId = user.getDepartment().getId();

        return courseOfferingRepository.findByCourse_Department_IdAndAcademicSemester_IsCurrentTrue(deptId)
                .stream()
                .map(offering -> {
                    long count = enrollmentRepository.countByCourseOffering(offering);

                    return DeptCourseDTO.builder()
                            .offeringId(offering.getId())
                            .courseName(offering.getCourse().getTitle())
                            .courseCode(offering.getCourse().getCourseCode())
                            .sectionName(offering.getSection().getName())
                            .instructorName(offering.getInstructor().getUser().getFirstName() + " "
                                    + offering.getInstructor().getUser().getMiddleName() + " "
                                    + offering.getInstructor().getUser().getLastName())
                            .semesterName(offering.getAcademicSemester().getName())
                            .studentCount(count)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DepartmentResponseDTO getDepartmentById(Long id) {
        Department dept = departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return mapEntityToDto(dept);
    }

    @Transactional
    public void deleteDepartment(Long id) {
        Department dept = departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        String oldData = dept.toString();
        dept.setDeleted(true);
        departmentRepository.save(dept);

        auditLogService.log("DELETE_DEPARTMENT", "Department", dept.getId(), oldData, "DELETED");
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getInstructorsByDepartment(Long departmentId) {
        List<User> instructors = userRepository.findByDepartmentIdAndRoleName(departmentId, "INSTRUCTOR");
        return instructors.stream()
                .map(this::mapToUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUnassignedInstructors() {
        return userRepository.findUnassignedByRole("INSTRUCTOR")
                .stream()
                .map(this::mapToUserResponseDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserResponseDTO assignStaffToDepartment(Long departmentId, AssignStaffRequestDTO dto,
            Authentication authentication) {

        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        User loggedInUser = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        boolean isSystemAdmin = loggedInUser.getRoles().stream()
                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));

        if (!isSystemAdmin) {
            boolean isDeptHead = loggedInUser.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("DEPARTMENT_HEAD"));

            if (!isDeptHead) {
                throw new AccessDeniedException("You are not a Department Head.");
            }

            if (loggedInUser.getDepartment() == null || !loggedInUser.getDepartment().getId().equals(departmentId)) {
                throw new AccessDeniedException("You are not the head of this department.");
            }
        }

        User staffToAssign = userRepository.findByIdAndIsDeletedFalse(dto.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User to assign not found with id: " + dto.getUserId()));

        if (staffToAssign.getDepartment() != null) {
            throw new IllegalArgumentException(
                    "This user is already assigned to department: " + staffToAssign.getDepartment().getName());
        }

        String oldData = staffToAssign.toString();
        staffToAssign.setDepartment(department);
        User savedStaff = userRepository.save(staffToAssign);

        auditLogService.log("ASSIGN_STAFF", "User", savedStaff.getId().longValue(), oldData, savedStaff.toString());

        return mapToUserResponseDTO(savedStaff);
    }

    @Transactional
    public UserResponseDTO assignDepartmentHead(Long departmentId, Long userId) {

        Department department = departmentRepository.findByIdAndIsDeletedFalse(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found"));

        User staffToAssign = userRepository.findByIdAndIsDeletedFalse(userId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        boolean isDeptHeadRole = staffToAssign.getRoles().stream()
                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));
        if (!isDeptHeadRole) {
            throw new IllegalArgumentException("User must have DEPARTMENT_HEAD role to be assigned as head.");
        }

        if (staffToAssign.getDepartment() != null && !staffToAssign.getDepartment().getId().equals(departmentId)) {
            throw new IllegalArgumentException("This user is already assigned to a different department: "
                    + staffToAssign.getDepartment().getName());
        }

        String oldDeptData = department.toString();
        String oldUserData = staffToAssign.toString();

        staffToAssign.setDepartment(department);

        department.setDepartmentHead(staffToAssign);

        User savedStaff = userRepository.save(staffToAssign);
        Department updatedDept = departmentRepository.save(department);
        auditLogService.log("ASSIGN_STAFF", "User", savedStaff.getId().longValue(), oldUserData, savedStaff.toString());
        auditLogService.log("ASSIGN_DEPARTMENT_HEAD", "Department", updatedDept.getId(),
                oldDeptData, updatedDept.toString());

        return mapToUserResponseDTO(savedStaff);

    }

    public DpHdDashboardStatsDTO getDashboardStats(Authentication authentication) {
        User user = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Department department = user.getDepartment();
        if (department == null)
            throw new AccessDeniedException("User has no department");

        DpHdDashboardStatsDTO statsDTO = new DpHdDashboardStatsDTO();

        statsDTO.setTotalStudents(studentRepository.countByDepartmentId(department.getId()));
        statsDTO.setTotalInstructors(instructorRepository.countByDepartmentId(department.getId()));
        statsDTO.setActiveCourses(courseOfferingRepository.countByDepartmentIdAndStatus(department.getId(), "ACTIVE"));
        statsDTO.setAvgDepartmentGpa(3.12);

        statsDTO.setEnrollmentByYear(Map.of(
                "Year 1", studentRepository.countByDepartmentIdAndYearLevel(department.getId(), 1),
                "Year 2", studentRepository.countByDepartmentIdAndYearLevel(department.getId(), 2),
                "Year 3", studentRepository.countByDepartmentIdAndYearLevel(department.getId(), 3),
                "Year 4", studentRepository.countByDepartmentIdAndYearLevel(department.getId(), 4)));

        statsDTO.setCourseStatusDistribution(Map.of(
                "Active", courseOfferingRepository.countByDepartmentIdAndStatus(department.getId(), "ACTIVE"),
                "Planned", courseOfferingRepository.countByDepartmentIdAndStatus(department.getId(), "PLANNED"),
                "Completed", courseOfferingRepository.countByDepartmentIdAndStatus(department.getId(), "COMPLETED")));

        Pageable top5 = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"));

        List<AuditLogResponse> logs = auditLogRepository.findByUser_Username(user.getUsername(), top5)
                .map(this::mapToAuditResponse) 
                .getContent();
        statsDTO.setRecentActivities(logs);

        return statsDTO;

    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUnassignedHeads() {
        return userRepository.findUnassignedByRole("DEPARTMENT_HEAD")
                .stream()
                .map(this::mapToUserResponseDTO)
                .collect(Collectors.toList());
    }
    
    public List<DepartmentSemesterGpaRawDto> getDepartmentSemesterGpa() {
        return enrollmentRepository.findDepartmentPerformanceStats();
    }
    // --- HELPER METHODS ---

    private UserResponseDTO mapToUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setMiddleName(user.getMiddleName());
        dto.setLastName(user.getLastName());
        dto.setDepartmentId(user.getDepartment() != null ? user.getDepartment().getId().intValue() : null);

        Set<UserResponseDTO.RoleResponse> roles = user.getRoles().stream()
                .map(role -> {
                    UserResponseDTO.RoleResponse rr = new UserResponseDTO.RoleResponse();
                    rr.setId(role.getId());
                    rr.setName(role.getName());
                    return rr;
                })
                .collect(Collectors.toSet());
        dto.setRoles(roles);

        return dto;
    }

    private DepartmentResponseDTO mapEntityToDto(Department entity) {
        DepartmentResponseDTO dto = new DepartmentResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setOfficeLocation(entity.getOfficeLocation());
        if (entity.getDepartmentHead() != null) {
            User head = entity.getDepartmentHead();
            dto.setDepartmentHeadId(head.getId());
            dto.setDepartmentHeadName(head.getFirstName() + " " + head.getMiddleName() + " " + head.getLastName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private AuditLogResponse mapToAuditResponse(com.hopesapms.app.modules.auditlog.model.AuditLog log) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(log.getId());

        if (log.getUser() != null) {
            dto.setUsername(log.getUser().getUsername());
        } else {
            dto.setUsername("System/Unknown");
        }

        dto.setActionType(log.getActionType());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setTimestamp(log.getTimestamp());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        
        
        return dto;
    }
}