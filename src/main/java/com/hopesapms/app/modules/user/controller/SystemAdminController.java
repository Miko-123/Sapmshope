package com.hopesapms.app.modules.user.controller;

import com.hopesapms.app.modules.user.dto.AdminDashboardStatsDTO;
import com.hopesapms.app.modules.auditlog.model.LoginLog;
import com.hopesapms.app.modules.auditlog.service.LoginLogService;
import com.hopesapms.app.modules.user.service.SystemAdminService;
import com.hopesapms.app.modules.user.service.SystemSettingsService;
import com.hopesapms.app.modules.notification.service.NotificationService;
import com.hopesapms.app.common.dto.MessageResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class SystemAdminController {

    private final LoginLogService loginLogService;
    private final SystemAdminService systemAdminService;
    private final SystemSettingsService systemSettingsService;
    private final NotificationService notificationService;

    @PostMapping("/login-history/maintenance/toggle")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<MessageResponseDTO> toggleMaintenance(@RequestParam boolean enable) {
        systemSettingsService.setMaintenanceMode(enable);
        String status = enable ? "ENABLED" : "DISABLED";
        return ResponseEntity.ok(new MessageResponseDTO("Maintenance mode is now " + status));
    }

    @GetMapping("/login-history/maintenance/status")
    public ResponseEntity<Boolean> getMaintenanceStatus() {
        return ResponseEntity.ok(systemSettingsService.isMaintenanceMode());
    }

    @PostMapping("/login-history/broadcast")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<MessageResponseDTO> sendBroadcast(
            @RequestParam String title,
            @RequestParam String message) {
        notificationService.sendBroadcast(title, message);
        return ResponseEntity.ok(new MessageResponseDTO("Broadcast sent to all active users."));
    }

    @GetMapping("/login-history")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Page<LoginLog>> getLoginHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return ResponseEntity.ok(loginLogService.getLoginHistory(pageable));
    }

    @GetMapping("/dashboard-stats")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<AdminDashboardStatsDTO> getDashboardStats() {
        return ResponseEntity.ok(systemAdminService.getDashboardStats());
    }
}