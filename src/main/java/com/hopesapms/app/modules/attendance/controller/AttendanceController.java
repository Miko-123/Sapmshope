package com.hopesapms.app.modules.attendance.controller;

import com.hopesapms.app.modules.attendance.dto.AttendanceSheetDTO;
import com.hopesapms.app.modules.attendance.dto.SaveAttendanceRequestDTO;
import com.hopesapms.app.modules.attendance.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Tag(name = "Attendance Management", description = "APIs for tracking student attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping("/{offeringId}")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Get attendance sheet for a specific date")
    public ResponseEntity<AttendanceSheetDTO> getSheet(
            @PathVariable Long offeringId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = (date != null) ? date : LocalDate.now();
        return ResponseEntity.ok(attendanceService.getSheet(offeringId, targetDate));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR')")
    @Operation(summary = "Save or update attendance")
    public ResponseEntity<Void> saveAttendance(@RequestBody SaveAttendanceRequestDTO dto) {
        attendanceService.saveAttendance(dto);
        return ResponseEntity.ok().build();
    }
}