package com.hopesapms.app.modules.courseoffering.controller;

import com.hopesapms.app.modules.courseoffering.dto.BulkCourseOfferingRequestDTO;
import com.hopesapms.app.modules.courseoffering.dto.CourseOfferingRequestDTO;
import com.hopesapms.app.modules.courseoffering.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.modules.schedule.dto.UpdateScheduleRequestDTO;
import com.hopesapms.app.modules.room.model.Room;
import com.hopesapms.app.modules.courseoffering.service.CourseOfferingService;
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

@RestController
@RequestMapping("/api/course-offerings")
@RequiredArgsConstructor
@Tag(name = "Course Offering Management", description = "APIs for scheduling courses (UC-007)")
public class CourseOfferingController {

    private final CourseOfferingService courseOfferingService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Create a new course offering (schedule a course)")
    public ResponseEntity<CourseOfferingResponseDTO> createCourseOffering(
            @Valid @RequestBody CourseOfferingRequestDTO dto,
            Authentication authentication) {

        CourseOfferingResponseDTO created = courseOfferingService.createCourseOffering(dto, authentication);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @PostMapping("/bulk-create")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD')")
    @Operation(summary = "Create a new course offering bulk")
    public ResponseEntity<List<CourseOfferingResponseDTO>> createBulkOfferings(
            @RequestBody BulkCourseOfferingRequestDTO dto,
            Authentication authentication) {
        return ResponseEntity.ok(courseOfferingService.createBulkOfferings(dto, authentication));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('DEPARTMENT_HEAD', 'SYSTEM_ADMIN')")
    @Operation(summary = "Update an existing course offering (Assign instructor, change status, etc.)")
    public ResponseEntity<CourseOfferingResponseDTO> updateCourseOffering(
            @PathVariable Long id,
            @Valid @RequestBody CourseOfferingRequestDTO dto,
            Authentication authentication) {

        CourseOfferingResponseDTO updated = courseOfferingService.updateCourseOffering(id, dto, authentication);
        return ResponseEntity.ok(updated);
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

    @PutMapping("/bulk")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Bulk update/create offerings for a specific course in a semester")
    public ResponseEntity<List<CourseOfferingResponseDTO>> bulkUpdateOfferings(
            @Valid @RequestBody BulkCourseOfferingRequestDTO dto,
            Authentication authentication) {

        return ResponseEntity.ok(courseOfferingService.bulkUpdateOfferings(dto, authentication));
    }

    @GetMapping("/available-rooms")
    @PreAuthorize("hasAnyAuthority('PROGRAM_OFFICER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<Room>> getAvailableRooms(
            @RequestParam Long semesterId,
            @RequestParam String day,
            @RequestParam String periods) {

        return ResponseEntity.ok(courseOfferingService.getAvailableRooms(semesterId, day, periods));
    }

    @GetMapping("/by-semester/{semesterId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all course offerings for a specific semester")
    public ResponseEntity<List<CourseOfferingResponseDTO>> getOfferingsBySemester(
            @PathVariable Long semesterId) {

        List<CourseOfferingResponseDTO> offerings = courseOfferingService
                .getOfferingsBySemester(semesterId);

        return ResponseEntity.ok(offerings);
    }

    @PostMapping("/copy-semester")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<String> copySemesterOfferings(
            @RequestParam Long sourceSemesterId,
            @RequestParam Long targetSemesterId,
            Authentication authentication) {

        int count = courseOfferingService.copyDepartmentOfferings(sourceSemesterId, targetSemesterId,
                authentication.getName());
        return ResponseEntity.ok("Successfully copied " + count + " course offerings and schedules.");
    }
}