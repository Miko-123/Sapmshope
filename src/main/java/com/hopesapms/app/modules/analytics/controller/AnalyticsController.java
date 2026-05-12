package com.hopesapms.app.modules.analytics.controller;

import com.hopesapms.app.modules.report.service.ReportService;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final ReportService reportService;
    private final UserRepository userRepository;

    @GetMapping("/department")
    @PreAuthorize("hasAuthority('DEPARTMENT_HEAD')")
    public ResponseEntity<?> getDeptAnalytics(Authentication auth) {
        User user = userRepository.findByEmailAndIsDeletedFalse(auth.getName()).orElseThrow();
        return ResponseEntity.ok(reportService.getDepartmentAnalytics(user.getDepartment().getId()));
    }

    @GetMapping("/registrar/trends")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    public ResponseEntity<?> getEnrollmentTrends() {
        return ResponseEntity.ok(reportService.getRegistrarEnrollmentTrends());
    }
}
