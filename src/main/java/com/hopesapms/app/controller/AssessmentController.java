package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AssessmentRequestDTO;
import com.hopesapms.app.dto.AssessmentResponseDTO;
import com.hopesapms.app.dto.PageRequestDTO;
import com.hopesapms.app.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
 
import java.util.List; 

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@Tag(name = "Assessment Management (Instructor)", description = "APIs for instructors to manage course assessments (UC-008)")
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Create a new assessment for a course")
    public ResponseEntity<AssessmentResponseDTO> createAssessment(
            @Valid @RequestBody AssessmentRequestDTO dto, Authentication authentication) {
        
        AssessmentResponseDTO created = assessmentService.createAssessment(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Update an existing assessment")
    public ResponseEntity<AssessmentResponseDTO> updateAssessment(
            @PathVariable Integer id, @Valid @RequestBody AssessmentRequestDTO dto, Authentication authentication) {
        
        AssessmentResponseDTO updated = assessmentService.updateAssessment(id, dto, authentication);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/by-course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all assessments for a course (Paginated)")
    public ResponseEntity<Page<AssessmentResponseDTO>> getAssessmentsForCourse(
            @PathVariable Integer courseId,
            @ParameterObject @PageableDefault(size = 10, page = 0) Pageable pageable) { 
        Page<AssessmentResponseDTO> assessments = assessmentService.getAssessmentsForCourse(courseId, pageable);
        return ResponseEntity.ok(assessments);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Soft-delete an assessment")
    public ResponseEntity<Void> deleteAssessment(@PathVariable Integer id, Authentication authentication) {
        assessmentService.deleteAssessment(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/my-assessments")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get all assessments for the logged-in student's active courses (UC-010)")
    public ResponseEntity<List<AssessmentResponseDTO>> getMyAssessments(Authentication authentication) {
        
        List<AssessmentResponseDTO> assessments = assessmentService.getMyAssessments(authentication);
        return ResponseEntity.ok(assessments);
    }
}