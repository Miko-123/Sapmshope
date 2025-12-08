package com.hopesapms.app.controller;

import com.hopesapms.app.dto.BulkEnrollmentRequestDTO;
import com.hopesapms.app.dto.EnrollmentRequestDTO;
import com.hopesapms.app.dto.EnrollmentResponseDTO;
import com.hopesapms.app.dto.ImportResultDTO;
import com.hopesapms.app.dto.MessageResponseDTO;
import com.hopesapms.app.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment Management (Student)", description = "APIs for students to enroll in courses (UC-011)")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/import")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Bulk enroll students from an Excel file (Registrar)")
    public ResponseEntity<ImportResultDTO> bulkEnroll(
            @RequestParam("file") MultipartFile file) {
        
        ImportResultDTO result = enrollmentService.bulkEnrollStudents(file);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'REGISTRAR')")
    public ResponseEntity<MessageResponseDTO> bulkEnroll(@RequestBody BulkEnrollmentRequestDTO dto) {
        String result = enrollmentService.bulkEnrollSection(dto);
        return ResponseEntity.ok(new MessageResponseDTO(result));
    }

    @PostMapping("/registrar")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Enroll a student in a course offering (Registrar)")
    public ResponseEntity<EnrollmentResponseDTO> enrollStudent(
            @Valid @RequestBody EnrollmentRequestDTO dto) {
        
        EnrollmentResponseDTO response = enrollmentService.enrollStudent(dto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/my-enrollments")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get all enrollments for the logged-in student (Paginated)")
    public ResponseEntity<Page<EnrollmentResponseDTO>> getMyEnrollments(
            Authentication authentication, 
            @ParameterObject Pageable pageable) { 
        
        Page<EnrollmentResponseDTO> enrollments = enrollmentService.getMyEnrollments(authentication, pageable);
        return ResponseEntity.ok(enrollments);
    }


}