package com.hopesapms.app.service;

import com.hopesapms.app.model.AuditLog;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.AuditLogRepository;
import com.hopesapms.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public void log(String actionType, String entityType, Integer entityId, String oldValue, String newValue) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsernameAndIsDeletedFalse(username).orElse(null);

        AuditLog log = AuditLog.builder()
                .user(user)
                .actionType(actionType)
                .entityType(entityType)
                .entityId(entityId)
                .oldValue(oldValue)
                .newValue(newValue)
                .build();

        auditLogRepository.save(log);

    }

}
