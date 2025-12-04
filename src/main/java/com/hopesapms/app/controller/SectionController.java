package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CreateSectionRequestDTO;
import com.hopesapms.app.dto.SectionResponseDTO;
import com.hopesapms.app.dto.UpdateSectionRequestDTO;
import com.hopesapms.app.service.SectionService;
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
@RequestMapping("/api/sections")
@RequiredArgsConstructor
@Tag(name = "Section Management", description = "APIs for managing class sections")
@PreAuthorize("hasAuthority('REGISTRAR')") 
public class SectionController {
    private final SectionService sectionService;

    @PostMapping
    @Operation(summary = "Create a new section (Registrar Only)")
    public ResponseEntity<SectionResponseDTO> createSection(
            @Valid @RequestBody CreateSectionRequestDTO dto) {
        SectionResponseDTO createdSection = sectionService.createSection(dto);
        return new ResponseEntity<>(createdSection, HttpStatus.CREATED);
    }

     @PutMapping("/{id}")
    @Operation(summary = "Update an existing section (Registrar Only)")
    public ResponseEntity<SectionResponseDTO> updateSection(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateSectionRequestDTO dto) {
        SectionResponseDTO updatedSection = sectionService.updateSection(id, dto);
        return ResponseEntity.ok(updatedSection);
    }

     @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a section (Registrar Only)")
    public ResponseEntity<Void> deleteSection(@PathVariable Integer id) {
        sectionService.deleteSection(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Get all active sections (For dropdowns)")
    @PreAuthorize("isAuthenticated()") 
    public ResponseEntity<List<SectionResponseDTO>> getAllSections() {
        return ResponseEntity.ok(sectionService.getAllSections());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single section by ID")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SectionResponseDTO> getSectionById(@PathVariable Integer id) {
        return ResponseEntity.ok(sectionService.getSectionById(id));
    }
}
