package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AcademicSemesterRequestDTO;
import com.hopesapms.app.dto.AcademicSemesterResponseDTO;
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
@RequestMapping("/api/academic-semesters")
@RequiredArgsConstructor
@Tag(name = "Academic Semester Management", description = "APIs for managing academic semesters (Admin Only)")
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')") // --- All methods are ADMIN ONLY ---
public class AcademicSemesterController {

    private final AcademicSemesterService semesterService;

    @PostMapping
    @Operation(summary = "Create a new academic semester (Admin Only)")
    public ResponseEntity<AcademicSemesterResponseDTO> createSemester(
            @Valid @RequestBody AcademicSemesterRequestDTO dto) {
        AcademicSemesterResponseDTO created = semesterService.createSemester(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()") // --- Allow anyone (students, instructors) to READ all semesters ---
    @Operation(summary = "Get all academic semesters")
    public ResponseEntity<List<AcademicSemesterResponseDTO>> getAllSemesters() {
        return ResponseEntity.ok(semesterService.getAllSemesters());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()") // --- Allow anyone to READ a single semester ---
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
    @Operation(summary = "Set a semester as the single 'current' semester (Admin Only)",
               description = "This will automatically unset any other semester that is currently active.")
    public ResponseEntity<AcademicSemesterResponseDTO> setCurrentSemester(@PathVariable Long id) {
        return ResponseEntity.ok(semesterService.setCurrentSemester(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete an academic semester (Admin Only)")
    public ResponseEntity<Void> deleteSemester(@PathVariable Long id) {
        semesterService.deleteSemester(id);
        return ResponseEntity.noContent().build();
    }
}