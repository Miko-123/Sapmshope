package com.hopesapms.app.service;

import com.hopesapms.app.dto.AssignStaffRequestDTO;
import com.hopesapms.app.dto.CreateDepartmentRequest;
import com.hopesapms.app.dto.DepartmentResponseDTO;
import com.hopesapms.app.dto.UpdateDepartmentDetailsRequest;
import com.hopesapms.app.dto.UserResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Department;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

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
            // This is your original authorization logic, now nested
            boolean isDeptHead = loggedInUser.getRoles().stream()
                    .anyMatch(r -> r.getName().equals("DEPARTMENT_HEAD"));

            if (!isDeptHead) {
                throw new AccessDeniedException("You are not a Department Head.");
            }

            if (loggedInUser.getDepartment() == null || !loggedInUser.getDepartment().getId().equals(departmentId)) {
                throw new AccessDeniedException("You are not the head of this department.");
            }
        }
        // --- END OF MODIFICATION ---

        // 5. Get the target user (the Instructor or Dept Head to be assigned)
        User staffToAssign = userRepository.findByIdAndIsDeletedFalse(dto.getUserId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("User to assign not found with id: " + dto.getUserId()));

        // 6. Business Logic: Check if they are truly unassigned
        if (staffToAssign.getDepartment() != null) {
            throw new IllegalArgumentException(
                    "This user is already assigned to department: " + staffToAssign.getDepartment().getName());
        }

        // 7. Assign and Save
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

    @Transactional(readOnly = true)
    public List<UserResponseDTO> getUnassignedHeads() {
        return userRepository.findUnassignedByRole("DEPARTMENT_HEAD")
                .stream()
                .map(this::mapToUserResponseDTO)
                .collect(Collectors.toList());
    }

    // --- HELPER METHODS ---

    private UserResponseDTO mapToUserResponseDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
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
        if (entity.getDepartmentHead() != null){
            User head = entity.getDepartmentHead();
            dto.setDepartmentHeadId(head.getId());
            dto.setDepartmentHeadName(head.getFirstName() + " " + head.getMiddleName());
        }
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}