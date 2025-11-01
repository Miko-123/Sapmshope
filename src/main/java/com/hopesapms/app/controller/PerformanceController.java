package com.hopesapms.app.controller;

import com.hopesapms.app.dto.SemesterPerformanceDTO;
import com.hopesapms.app.service.PerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
@Tag(name = "Student Performance", description = "APIs for students to view their performance (UC-012)")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("/my-summary")
    @PreAuthorize("hasAuthority('STUDENT')")
    @Operation(summary = "Get my semester-wide performance and GPA (UC-012)")
    public ResponseEntity<SemesterPerformanceDTO> getMyPerformance(Authentication authentication) {
        
        SemesterPerformanceDTO performance = performanceService.getMyPerformance(authentication);
        return ResponseEntity.ok(performance);
    }
}