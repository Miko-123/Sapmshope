/*package com.hopesapms.app.controller;

import com.hopesapms.app.modules.schedule.dto.ClassSessionRequestDTO;
import com.hopesapms.app.modules.schedule.dto.ClassSessionResponseDTO;
import com.hopesapms.app.modules.schedule.service.ClassSessionService;
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
@RequestMapping("/api/class-sessions")
@RequiredArgsConstructor
@Tag(name = "Class Session Management (Admin/Instructor)", description = "APIs to create and manage class sessions")
public class ClassSessionController {

    private final ClassSessionService classSessionService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Create a new class session for a course")
    public ResponseEntity<ClassSessionResponseDTO> createClassSession(
            @Valid @RequestBody ClassSessionRequestDTO dto, Authentication authentication) {
        ClassSessionResponseDTO created = classSessionService.createClassSession(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/by-course/{courseId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all class sessions for a course (Paginated)")
    public ResponseEntity<Page<ClassSessionResponseDTO>> getSessionsByCourse(
            @PathVariable Integer courseId, Pageable pageable) {
        return ResponseEntity.ok(classSessionService.getClassSessionsByCourseId(courseId, pageable));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Soft-delete a class session")
    public ResponseEntity<Void> deleteClassSession(@PathVariable Integer id, Authentication authentication) {
        classSessionService.deleteClassSession(id, authentication);
        return ResponseEntity.noContent().build();
    }
}*/