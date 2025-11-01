package com.hopesapms.app.controller;

import com.hopesapms.app.dto.PrerequisiteRequestDTO;
import com.hopesapms.app.dto.PrerequisiteResponseDTO;
import com.hopesapms.app.service.PrerequisiteService;
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
@RequestMapping("/api/prerequisites")
@RequiredArgsConstructor
@Tag(name = "Prerequisite Management", description = "APIs for managing course prerequisites (Part of UC-007)")
public class PrerequisiteController {

    private final PrerequisiteService prerequisiteService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Create a new course prerequisite")
    public ResponseEntity<PrerequisiteResponseDTO> createPrerequisite(
            @Valid @RequestBody PrerequisiteRequestDTO dto, Authentication authentication) {
        
        PrerequisiteResponseDTO created = prerequisiteService.createPrerequisite(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/for-course/{courseId}")
    @PreAuthorize("isAuthenticated()") 
    @Operation(summary = "Get all prerequisites for a specific course")
    public ResponseEntity<List<PrerequisiteResponseDTO>> getPrerequisitesForCourse(@PathVariable Integer courseId) {
        List<PrerequisiteResponseDTO> prerequisites = prerequisiteService.getPrerequisitesForCourse(courseId);
        return ResponseEntity.ok(prerequisites);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Delete a prerequisite link")
    public ResponseEntity<Void> deletePrerequisite(@PathVariable Integer id, Authentication authentication) {
        prerequisiteService.deletePrerequisite(id, authentication);
        return ResponseEntity.noContent().build();
    }
}