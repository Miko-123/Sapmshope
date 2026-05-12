package com.hopesapms.app.modules.analytics.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hopesapms.app.modules.analytics.dto.CohortAnalyticsDTO;
import com.hopesapms.app.modules.department.dto.DepartmentGraduationStatusDTO;
import com.hopesapms.app.modules.analytics.dto.GraduationStatusSummaryDTO;
import com.hopesapms.app.modules.analytics.service.GraduationAnalyticsService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/graduation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'VICE_PRESIDENT')")
public class GraduationDashboardController {

    private final GraduationAnalyticsService service;

    @GetMapping("/status-summary")
    public ResponseEntity<GraduationStatusSummaryDTO> statusSummary(@RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(service.getStatusSummary(semesterId));
    }

    @GetMapping("/by-department")
    public ResponseEntity<List<DepartmentGraduationStatusDTO>> departmentStatus(@RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(service.getDepartmentStatus(semesterId));
    }

    @GetMapping("/cohorts")
    public ResponseEntity<List<CohortAnalyticsDTO>> cohorts(@RequestParam(required = false) Long semesterId) {
        return ResponseEntity.ok(service.getCohorts(semesterId));
    }
}