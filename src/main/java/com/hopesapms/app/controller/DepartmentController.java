package com.hopesapms.app.controller;

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
import org.springframework.security.core.Authentication; // Import this
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Tag(name = "Department Management", description = "APIs for managing academic departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new department (Admin Only)", description = "Creates the initial department shell with just a name.")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(@Valid @RequestBody CreateDepartmentRequest dto) {
        DepartmentResponseDTO createdDept = departmentService.createDepartment(dto);
        return new ResponseEntity<>(createdDept, HttpStatus.CREATED);
    }

    @PutMapping("/my-department/details")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Update my department details (Dept Head Only)", description = "Allows a Department Head to fill in the details for their *own* assigned department.")
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
}