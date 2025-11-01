package com.hopesapms.app.controller;

import com.hopesapms.app.dto.GradebookDTO;
import com.hopesapms.app.service.GradebookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/gradebook")
@RequiredArgsConstructor
@Tag(name = "Gradebook (Instructor)", description = "APIs for instructors to view course gradebooks (UC-009)")
public class GradebookController {

    private final GradebookService gradebookService;

    @GetMapping("/{courseId}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Get the full gradebook for a course")
    public ResponseEntity<GradebookDTO> getGradebookForCourse(
            @PathVariable Integer courseId, Authentication authentication) {
        
        GradebookDTO gradebook = gradebookService.getGradebookForCourse(courseId, authentication);
        return ResponseEntity.ok(gradebook);
    }
}