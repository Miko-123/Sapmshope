package com.hopesapms.app.controller;

import com.hopesapms.app.dto.ProgramRequestDTO;
import com.hopesapms.app.dto.ProgramResponseDTO;
import com.hopesapms.app.service.ProgramService;
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
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequestMapping("/api/programs")
@RequiredArgsConstructor
@Tag(name = "Program Management", description = "APIs for managing academic programs (UC-007)")
public class ProgramController {

    private final ProgramService programService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Create a new academic program")
    public ResponseEntity<ProgramResponseDTO> createProgram(
            @Valid @RequestBody ProgramRequestDTO dto, Authentication authentication) {
        
        ProgramResponseDTO created = programService.createProgram(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all active academic programs")
    public ResponseEntity<List<ProgramResponseDTO>> getAllPrograms() {
        return ResponseEntity.ok(programService.getAllPrograms());
    }

    @GetMapping("/my-department")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<List<ProgramResponseDTO>> getMyDepartmentPrograms (Authentication authentication) {
        return ResponseEntity.ok(programService.getMyDepartmentPrograms(authentication));
    }
    

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a single program by its ID")
    public ResponseEntity<ProgramResponseDTO> getProgramById(@PathVariable Long id) {
        return ResponseEntity.ok(programService.getProgramById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Update an existing academic program")
    public ResponseEntity<ProgramResponseDTO> updateProgram(
            @PathVariable Long id, @Valid @RequestBody ProgramRequestDTO dto, Authentication authentication) {
        
        ProgramResponseDTO updated = programService.updateProgram(id, dto, authentication);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Soft-delete an academic program")
    public ResponseEntity<Void> deleteProgram(@PathVariable Long id, Authentication authentication) {
        programService.deleteProgram(id, authentication);
        return ResponseEntity.noContent().build();
    }
}