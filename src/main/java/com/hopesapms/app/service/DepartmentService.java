package com.hopesapms.app.service;

import com.hopesapms.app.dto.CreateDepartmentRequest;
import com.hopesapms.app.dto.DepartmentResponseDTO;
import com.hopesapms.app.dto.UpdateDepartmentDetailsRequest;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository; // To find the logged-in user
    private final AuditLogService auditLogService;

    /**
     * Called by SYSTEM_ADMIN.
     * Creates a new Department with *only* its name.
     */
    @Transactional
    public DepartmentResponseDTO createDepartment(CreateDepartmentRequest dto) {
        // Business Logic: Check for uniqueness
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
    public DepartmentResponseDTO updateMyDepartmentDetails(UpdateDepartmentDetailsRequest dto, Authentication authentication) {

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


    private DepartmentResponseDTO mapEntityToDto(Department entity) {
        DepartmentResponseDTO dto = new DepartmentResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setCode(entity.getCode());
        dto.setContactEmail(entity.getContactEmail());
        dto.setContactPhone(entity.getContactPhone());
        dto.setOfficeLocation(entity.getOfficeLocation());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}