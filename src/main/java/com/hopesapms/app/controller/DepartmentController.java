package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AssignStaffRequestDTO;
import com.hopesapms.app.dto.UserResponseDTO;
import com.hopesapms.app.dto.CreateDepartmentRequest;
import com.hopesapms.app.dto.DepartmentResponseDTO;
import com.hopesapms.app.dto.UpdateDepartmentDetailsRequest;
import com.hopesapms.app.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication; 
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "APIs for managing academic departments and staff") 
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new department (Admin Only)")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody CreateDepartmentRequest dto) {
        DepartmentResponseDTO createdDept = departmentService.createDepartment(dto);
        return new ResponseEntity<>(createdDept, HttpStatus.CREATED);
    }

    @PutMapping("/my-department/details")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Update my department details (Dept Head Only)")
    public ResponseEntity<DepartmentResponseDTO> updateMyDepartmentDetails(
            @Valid @RequestBody UpdateDepartmentDetailsRequest dto, Authentication authentication) {
        
        DepartmentResponseDTO updatedDept = departmentService.updateMyDepartmentDetails(dto, authentication);
        return ResponseEntity.ok(updatedDept);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") 
    @Operation(summary = "Get all active departments")
    public ResponseEntity<List<DepartmentResponseDTO>> getAllDepartments() {
        return ResponseEntity.ok(departmentService.getAllDepartments());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") 
    @Operation(summary = "Get a single department by ID")
    public ResponseEntity<DepartmentResponseDTO> getDepartmentById(@PathVariable Long id) {
        return ResponseEntity.ok(departmentService.getDepartmentById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Soft-delete a department (Admin Only)")
    public ResponseEntity<Void> deleteDepartment(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.noContent().build();
    }


    // --- ADD THESE NEW ENDPOINTS FOR STAFF ASSIGNMENT ---

    @GetMapping("/unassigned-instructors")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Get list of instructors not assigned to any department (Dept Head Only)")
    public ResponseEntity<List<UserResponseDTO>> getUnassignedInstructors() {
        return ResponseEntity.ok(departmentService.getUnassignedInstructors());
    }

    @PutMapping("/{departmentId}/assign-staff")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'SYSTEM_ADMIN')")
    @Operation(summary = "Assign an unassigned staff member to your department (Dept Head Only)")
    public ResponseEntity<UserResponseDTO> assignStaff(
            @PathVariable Long departmentId,
            @Valid @RequestBody AssignStaffRequestDTO dto,
            Authentication authentication) {
        
        UserResponseDTO assignedUser = departmentService.assignStaffToDepartment(departmentId, dto, authentication);
        return ResponseEntity.ok(assignedUser);
    }
}