package com.hopesapms.app.controller;

import com.hopesapms.app.dto.InstructorAttendanceDTO;
import com.hopesapms.app.dto.MarkInstructorRequest;
import com.hopesapms.app.service.ProgramOfficerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/program-officer")
@RequiredArgsConstructor
@Tag(name = "Program Officer", description = "Attendance taking for staff")
public class ProgramOfficerController {

    private final ProgramOfficerService service;

    @GetMapping("/classes/today")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'DEPARTMENT_HEAD', 'PROGRAM_OFFICER')")
    @Operation(summary = "Get all classes scheduled for today")
    public ResponseEntity<List<InstructorAttendanceDTO>> getTodaysClasses() {
        return ResponseEntity.ok(service.getTodaysClasses());
    }

    @PostMapping("/attendance")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'DEPARTMENT_HEAD', 'PROGRAM_OFFICER')")
    @Operation(summary = "Mark instructor as Present/Absent")
    public ResponseEntity markAttendance(
            @RequestBody MarkInstructorRequest request,
            Authentication authentication) {

        service.markInstructorAttendance(request, authentication.getName());
        return ResponseEntity.ok().build();
    }
}