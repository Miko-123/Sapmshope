package com.hopesapms.app.modules.auditlog.controller;

import com.hopesapms.app.modules.auditlog.dto.AuditLogResponse;
import com.hopesapms.app.modules.auditlog.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,      
            @RequestParam(required = false) String actionType,  
            @RequestParam(required = false) String entityType   
    ) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return ResponseEntity.ok(auditLogQueryService.getLogs(search, actionType, entityType, pageRequest));
    }
}