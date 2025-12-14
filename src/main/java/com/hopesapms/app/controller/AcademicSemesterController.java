package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AcademicSemesterRequestDTO;
import com.hopesapms.app.dto.AcademicSemesterResponseDTO;
import com.hopesapms.app.dto.SemesterRolloverRequest;
import com.hopesapms.app.service.AcademicSemesterService;
import com.hopesapms.app.service.SemesterRolloverService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/academic-semesters")
@RequiredArgsConstructor
@Tag(name = "Academic Semester Management", description = "APIs for managing academic semesters (Admin Only)")
public class AcademicSemesterController {

    private final AcademicSemesterService semesterService;
    private final SemesterRolloverService rolloverService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'REGISTRAR')")
    @Operation(summary = "Create a new academic semester")
    public ResponseEntity<AcademicSemesterResponseDTO> createSemester(
            @Valid @RequestBody AcademicSemesterRequestDTO dto) {
        AcademicSemesterResponseDTO created = semesterService.createSemester(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all academic semesters")
    public ResponseEntity<List<AcademicSemesterResponseDTO>> getAllSemesters() {
        return ResponseEntity.ok(semesterService.getAllSemesters());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a single academic semester by ID")
    public ResponseEntity<AcademicSemesterResponseDTO> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getSemesterById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an academic semester (Admin Only)")
    public ResponseEntity<AcademicSemesterResponseDTO> updateSemester(
            @PathVariable Long id, @Valid @RequestBody AcademicSemesterRequestDTO dto) {
        return ResponseEntity.ok(semesterService.updateSemester(id, dto));
    }

    @PutMapping("/{id}/set-current")
    @Operation(summary = "Set a semester as the single 'current' semester (Admin Only)", description = "This will automatically unset any other semester that is currently active.")
    public ResponseEntity<AcademicSemesterResponseDTO> setCurrentSemester(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.setCurrentSemester(id));
    }

    @PostMapping("/rollover")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'REGISTRAR')")
    @Operation(summary = "Copy course offerings from one semester to another", description = "Creates new offering rows for the target semester. Does NOT copy students or grades.")
    public ResponseEntity<String> rolloverSemester(@RequestBody SemesterRolloverRequest request) {
        int count = rolloverService.rolloverSemester(request);
        return ResponseEntity.ok("Rollover successful. " + count + " course offerings created for the new semester.");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an academic semester (Admin Only)")
    public ResponseEntity<Void> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.noContent().build();
    }
}