package com.hopesapms.app.controller;

import com.hopesapms.app.dto.EnrollmentRequestDTO;
import com.hopesapms.app.dto.EnrollmentResponseDTO;
import com.hopesapms.app.service.EnrollmentService;
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

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Management (Student)", description = "APIs for students to enroll in courses (UC-011)")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Enroll the logged-in student in a course")
    public ResponseEntity<EnrollmentResponseDTO> enrollInCourse(
            @Valid @RequestBody EnrollmentRequestDTO dto, Authentication authentication) {
        
        EnrollmentResponseDTO enrollment = enrollmentService.enrollInCourse(dto, authentication);
        return new ResponseEntity<>(enrollment, HttpStatus.CREATED);
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get all enrollments for the logged-in student (Paginated)")
    public ResponseEntity<Page<EnrollmentResponseDTO>> getMyEnrollments(
            Authentication authentication, Pageable pageable) { // Added Pageable
        
        Page<EnrollmentResponseDTO> enrollments = enrollmentService.getMyEnrollments(authentication, pageable);
        return ResponseEntity.ok(enrollments);
    }
}