package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AcademicSemesterDTO;
import com.hopesapms.app.service.AcademicSemesterService;
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
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
@Tag(name = "Academic Semester Management", description = "APIs for managing academic semesters (Admin only)")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')") 
public class AcademicSemesterController {

    private final AcademicSemesterService semesterService;

    @PostMapping
    @Operation(summary = "Create a new academic semester")
    public ResponseEntity<AcademicSemesterDTO> createSemester(@Valid @RequestBody AcademicSemesterDTO dto) {
        AcademicSemesterDTO created = semesterService.createSemester(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "Get all active academic semesters")
    public ResponseEntity<List<AcademicSemesterDTO>> getAllSemesters() {
        return ResponseEntity.ok(semesterService.getAllSemesters());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single semester by ID")
    public ResponseEntity<AcademicSemesterDTO> getSemesterById(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.getSemesterById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing academic semester")
    public ResponseEntity<AcademicSemesterDTO> updateSemester(@PathVariable Long id, @Valid @RequestBody AcademicSemesterDTO dto) {
        return ResponseEntity.ok(semesterService.updateSemester(id, dto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an academic semester")
    public ResponseEntity<Void> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.noContent().build();
    }
}