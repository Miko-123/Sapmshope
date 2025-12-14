package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CreateGradeChangeRequest;
import com.hopesapms.app.dto.GradeChangeRequestDTO;
import com.hopesapms.app.service.GradeChangeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades/requests")
@RequiredArgsConstructor
public class GradeChangeController {

    private final GradeChangeService service;

    @PostMapping
    @PreAuthorize("hasAuthority('INSTRUCTOR')")
    @Operation(summary = "Submit a grade change request for locked grades")
    public ResponseEntity<GradeChangeRequestDTO> requestChange(
            @Valid @RequestBody CreateGradeChangeRequest dto, 
            Authentication authentication) {
        return ResponseEntity.ok(service.requestChange(dto, authentication));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'REGISTRAR')")
    @Operation(summary = "Get all pending grade change requests")
    public ResponseEntity<Page<GradeChangeRequestDTO>> getPendingRequests(Pageable pageable) {
        return ResponseEntity.ok(service.getPendingRequests(pageable));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'REGISTRAR')")
    public ResponseEntity<GradeChangeRequestDTO> approve(
            @PathVariable Long id, 
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        return ResponseEntity.ok(service.approveRequest(id, comment, authentication));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'REGISTRAR')")
    public ResponseEntity<GradeChangeRequestDTO> reject(
            @PathVariable Long id, 
            @RequestParam(required = false) String comment,
            Authentication authentication) {
        return ResponseEntity.ok(service.rejectRequest(id, comment, authentication));
    }
}