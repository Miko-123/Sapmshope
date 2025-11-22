package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.CourseOfferingSearchResultDTO;
import com.hopesapms.app.service.CourseOfferingService;
import com.hopesapms.app.service.CourseOfferingImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; 
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile; 

import java.util.List;
import java.util.Map; 

@RestController
@RequestMapping("/api/course-offerings")
@RequiredArgsConstructor
@Tag(name = "Course Offering Management", description = "APIs for scheduling courses (UC-007)")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;
    private final CourseOfferingImportService courseOfferingImportService; 

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'REGISTRAR')")
    @Operation(summary = "Create a new course offering (schedule a course)")
    public ResponseEntity<CourseOfferingResponseDTO> createCourseOffering(
            @Valid @RequestBody CourseOfferingRequestDTO dto) {
        
        CourseOfferingResponseDTO created = courseOfferingService.createCourseOffering(dto);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'REGISTRAR')")
    @Operation(summary = "Import course offerings from an Excel file")
    public ResponseEntity<Map<String, Object>> importOfferings(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PLANNED") String status) {
                
        Map<String, Object> result = courseOfferingImportService.importOfferings(file, status);
        
        if (result.get("errors") != null && !((List)result.get("errors")).isEmpty()) {
            
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/by-semester/{semesterId}")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR', 'STUDENT')")
    @Operation(summary = "Get all course offerings for a specific semester")
    public ResponseEntity<List<CourseOfferingSearchResultDTO>> getOfferingsBySemester(
            @PathVariable Long semesterId) { // <-- MUST BE Long
        
        List<CourseOfferingSearchResultDTO> offerings = courseOfferingService
            .getOfferingsBySemester(semesterId);
        
        return ResponseEntity.ok(offerings);
    }
}