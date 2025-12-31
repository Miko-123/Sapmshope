package com.hopesapms.app.controller;

import com.hopesapms.app.dto.InstructorAttendanceDashboardDTO;
import com.hopesapms.app.service.InstructorAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/instructor-attendance")
@RequiredArgsConstructor
@Tag(name = "Instructor Analytics", description = "Vice President Analytics for Faculty")
public class InstructorAnalyticsController {

    private final InstructorAnalyticsService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VICE_PRESIDENT', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get instructor attendance metrics and trends")
    public ResponseEntity<InstructorAttendanceDashboardDTO> getDashboardData() {
        return ResponseEntity.ok(service.getDashboardData());
    }
}