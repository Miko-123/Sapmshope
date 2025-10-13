package com.hopesapms.app.controller;

import com.hopesapms.app.dto.AuditLogResponse;
import com.hopesapms.app.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(auditLogQueryService.getLogs(page, size));
    }
}
