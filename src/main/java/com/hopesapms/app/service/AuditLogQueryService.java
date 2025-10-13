package com.hopesapms.app.service;

import com.hopesapms.app.dto.AuditLogResponse;
import com.hopesapms.app.model.AuditLog;
import com.hopesapms.app.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> getLogs(int page, int size) {
        return auditLogRepository.findAll(PageRequest.of(page, size))
                .map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        AuditLogResponse dto = new AuditLogResponse();
        dto.setId(log.getId());
        dto.setUsername(log.getUser() != null ? log.getUser().getUsername() : "SYSTEM");
        dto.setActionType(log.getActionType());
        dto.setEntityType(log.getEntityType());
        dto.setEntityId(log.getEntityId());
        dto.setTimestamp(log.getTimestamp());
        dto.setOldValue(log.getOldValue());
        dto.setNewValue(log.getNewValue());
        return dto;
    }
}
