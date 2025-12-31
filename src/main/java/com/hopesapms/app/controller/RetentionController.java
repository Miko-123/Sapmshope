// File: src/main/java/com/hopesapms/app/controller/RetentionController.java
package com.hopesapms.app.controller;

import com.hopesapms.app.dto.RetentionDashboardDTO;
import com.hopesapms.app.service.RetentionAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/retention")
@RequiredArgsConstructor
@Tag(name = "Retention Analytics", description = "Vice President Analytics Dashboard")
public class RetentionController {

    private final RetentionAnalyticsService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('VICE_PRESIDENT', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get retention dashboard statistics")
    public ResponseEntity<RetentionDashboardDTO> getDashboardStats() {
        return ResponseEntity.ok(service.getDashboardData());
    }
}