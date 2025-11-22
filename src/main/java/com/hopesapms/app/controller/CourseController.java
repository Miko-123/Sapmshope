package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CourseRequestDTO;
import com.hopesapms.app.dto.CourseResponseDTO;
import com.hopesapms.app.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Course Management", description = "APIs for managing courses (UC-007)")
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Create a single new course")
    public ResponseEntity<CourseResponseDTO> createCourse(
            @Valid @RequestBody CourseRequestDTO dto, Authentication authentication) {
        
        CourseResponseDTO created = courseService.createCourse(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Bulk-create courses for the Department Head's department",
                 description = "Takes a list of courses. The departmentId on all DTOs will be *ignored* and replaced with the user's assigned department.")
    public ResponseEntity<List<CourseResponseDTO>> bulkCreateCourses(
            @Valid @RequestBody List<CourseRequestDTO> dtoList, Authentication authentication) {
        
        List<CourseResponseDTO> created = courseService.bulkCreateCourses(dtoList, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/by-department/{departmentId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a paginated list of courses for a department")
    public ResponseEntity<Page<CourseResponseDTO>> getCoursesByDepartment(
            @PathVariable Long departmentId, Pageable pageable) {
        
        Page<CourseResponseDTO> courses = courseService.getCoursesByDepartment(departmentId, pageable);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a single course by its ID")
    public ResponseEntity<CourseResponseDTO> getCourseById(@PathVariable Integer id) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Soft-delete a course")
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id, Authentication authentication) {
        courseService.deleteCourse(id, authentication);
        return ResponseEntity.noContent().build();
    }
}