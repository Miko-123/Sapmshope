package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AssignStaffRequestDTO;
import com.hopesapms.app.dto.UserResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.UserRepository;
import com.hopesapms.app.dto.CreateDepartmentRequest;
import com.hopesapms.app.dto.DepartmentAttendanceReportDTO;
import com.hopesapms.app.dto.DepartmentResponseDTO;
import com.hopesapms.app.dto.DeptCourseDTO;
import com.hopesapms.app.dto.DpHdDashboardStatsDTO;
import com.hopesapms.app.dto.ProgramResponseDTO;
import com.hopesapms.app.dto.UpdateDepartmentDetailsRequest;
import com.hopesapms.app.service.DepartmentService;
import com.hopesapms.app.service.ProgramService;
import com.hopesapms.app.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final ProgramService programService;
    private final ReportService reportService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new department (Admin Only)")
    public ResponseEntity<DepartmentResponseDTO> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest dto) {
        DepartmentResponseDTO createdDept = departmentService.createDepartment(dto);
        return new ResponseEntity<>(createdDept, HttpStatus.CREATED);
    }

    @GetMapping("/{departmentId}/programs")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all programs for a specific department")
    public ResponseEntity<List<ProgramResponseDTO>> getProgramsByDepartment(@PathVariable Long departmentId) {
        List<ProgramResponseDTO> programs = programService.getProgramsByDepartmentId(departmentId);
        return ResponseEntity.ok(programs);
    }

    @GetMapping("/courses")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<List<DeptCourseDTO>> getDepartmentCourses(Authentication authentication) {
        return ResponseEntity.ok(departmentService.getCoursesForDeptHead(authentication));
    }

    @GetMapping("/{departmentId}/instructors")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'PROGRAM_OFFICER')")
    @Operation(summary = "Get all instructors for a specific department")
    public ResponseEntity<List<UserResponseDTO>> getInstructorsByDepartment(@PathVariable Long departmentId) {
        List<UserResponseDTO> instructors = departmentService.getInstructorsByDepartment(departmentId);
        return ResponseEntity.ok(instructors);
    }

    @PutMapping("/{departmentId}/assign-head/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Assign a Department Head to a department (Admin Only)")
    public ResponseEntity<UserResponseDTO> assignDepartmentHead(
            @PathVariable Long departmentId,
            @PathVariable Long userId) {

        UserResponseDTO assignedHead = departmentService.assignDepartmentHead(departmentId, userId);
        return ResponseEntity.ok(assignedHead);
    }

    @GetMapping("/unassigned-heads")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Get list of department heads not assigned to any department (Admin Only)")
    public ResponseEntity<List<UserResponseDTO>> getUnassignedHeads() {
        return ResponseEntity.ok(departmentService.getUnassignedHeads());
    }

    @GetMapping("/my-department")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<DepartmentResponseDTO> getMyDepartment(Authentication authentication) {
        return ResponseEntity.ok(departmentService.getMyDepartment(authentication));
    }

    @PutMapping("/my-department/details")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Update my department details (Dept Head Only)")
    public ResponseEntity<DepartmentResponseDTO> updateMyDepartmentDetails(
            @Valid @RequestBody UpdateDepartmentDetailsRequest dto, Authentication authentication) {

        DepartmentResponseDTO updatedDept = departmentService.updateMyDepartmentDetails(dto, authentication);
        return ResponseEntity.ok(updatedDept);
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<DpHdDashboardStatsDTO> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(departmentService.getDashboardStats(authentication));
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

    @GetMapping("/reports/attendance/instructors")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Get instructor attendance report for the department")
    public ResponseEntity<List<DepartmentAttendanceReportDTO.InstructorStats>> getInstructorAttendanceReport(
            Authentication authentication) {
        User user = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getDepartment() == null) {
            throw new AccessDeniedException("User is not assigned to a department");
        }

        return ResponseEntity.ok(reportService.getDepartmentInstructorAttendance(user.getDepartment().getId()));
    }

    @GetMapping("/reports/attendance/students")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Get student attendance report for the department")
    public ResponseEntity<List<DepartmentAttendanceReportDTO.StudentStats>> getStudentAttendanceReport(
            Authentication authentication) {
        User user = userRepository.findByEmailAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getDepartment() == null) {
            throw new AccessDeniedException("User is not assigned to a department");
        }

        return ResponseEntity.ok(reportService.getDepartmentStudentAttendance(user.getDepartment().getId()));
    }
}