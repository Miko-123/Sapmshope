package com.hopesapms.app.controller;

import com.hopesapms.app.dto.BulkCourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.UpdateScheduleRequestDTO;
import com.hopesapms.app.model.Room;
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
import org.springframework.security.core.Authentication;
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
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'PROGRAM_OFFICER')")
    @Operation(summary = "Create a new course offering (schedule a course)")
    public ResponseEntity<CourseOfferingResponseDTO> createCourseOffering(
            @Valid @RequestBody CourseOfferingRequestDTO dto,
            Authentication authentication
        ) {
        
        CourseOfferingResponseDTO created = courseOfferingService.createCourseOffering(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/bulk-create")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a new course offering bulk")
    public ResponseEntity<List<CourseOfferingResponseDTO>> createBulkOfferings(
            @RequestBody BulkCourseOfferingRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(courseOfferingService.createBulkOfferings(dto, authentication));
    }

     @PutMapping("/{id}/schedule")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'PROGRAM_OFFICER')")
    @Operation(summary = "Update schedule slots and status (Program Officer)")
    public ResponseEntity<CourseOfferingResponseDTO> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody UpdateScheduleRequestDTO dto) {
        
        CourseOfferingResponseDTO updated = courseOfferingService.updateCourseSchedule(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/available-rooms")
    @PreAuthorize("hasAnyAuthority('PROGRAM_OFFICER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<Room>> getAvailableRooms(
            @RequestParam Long semesterId,
            @RequestParam String day,
            @RequestParam String periods) {
        
        return ResponseEntity.ok(courseOfferingService.getAvailableRooms(semesterId, day, periods));
    }

   /*  @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN','DEPARTMENT_HEAD', 'PROGRAM_OFFICER')")
    @Operation(summary = "Import course offerings from an Excel file")
    public ResponseEntity<Map<String, Object>> importOfferings(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "PLANNED") String status,
            Authentication authentication
        ) {
                
        Map<String, Object> result = courseOfferingImportService.importOfferings(file, status, authentication);
        
        if (result.get("errors") != null && !((List)result.get("errors")).isEmpty()) {
            
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        }
        
        return ResponseEntity.ok(result);
    }
    */
    @GetMapping("/by-semester/{semesterId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all course offerings for a specific semester")
    public ResponseEntity<List<CourseOfferingResponseDTO>> getOfferingsBySemester(
            @PathVariable Long semesterId) { 
        
        List<CourseOfferingResponseDTO> offerings = courseOfferingService
            .getOfferingsBySemester(semesterId);
        
        return ResponseEntity.ok(offerings);
    }
}