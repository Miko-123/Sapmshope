package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AssessmentRequestDTO;
import com.hopesapms.app.dto.AssessmentResponseDTO;
import com.hopesapms.app.service.AssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Create a new assessment for a course offering")
    public ResponseEntity<AssessmentResponseDTO> createAssessment(
            @Valid @RequestBody AssessmentRequestDTO dto, Authentication authentication) {

        AssessmentResponseDTO created = assessmentService.createAssessment(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Update an existing assessment")
    public ResponseEntity<AssessmentResponseDTO> updateAssessment(
            @PathVariable Long id, 
            @Valid @RequestBody AssessmentRequestDTO dto, 
            Authentication authentication) {

        AssessmentResponseDTO updated = assessmentService.updateAssessment(id, dto, authentication);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/by-offering/{offeringId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all assessments for a specific course offering (Paginated)")
    public ResponseEntity<Page<AssessmentResponseDTO>> getAssessmentsForOffering(
            @PathVariable Long offeringId, Pageable pageable) {

        Page<AssessmentResponseDTO> assessments = assessmentService.getAssessmentsForOffering(offeringId, pageable);
        return ResponseEntity.ok(assessments);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Soft-delete an assessment")
    public ResponseEntity<Void> deleteAssessment(
            @PathVariable Long id, 
            Authentication authentication) {
        
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