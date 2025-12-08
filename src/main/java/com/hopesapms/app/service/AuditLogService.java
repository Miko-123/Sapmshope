package com.hopesapms.app.service;

import com.hopesapms.app.model.AuditLog;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.AuditLogRepository;
import com.hopesapms.app.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String actionType, String entityType, Long entityId, String oldValue, String newValue) {

        User user = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    
        if (authentication != null 
                && !(authentication instanceof AnonymousAuthenticationToken) 
                && authentication.isAuthenticated()) {
            
            String identifier = authentication.getName();

            user = userRepository.findByEmailAndIsDeletedFalse(identifier).orElse(null);

            if (user == null) {
                user = userRepository.findByUsernameAndIsDeletedFalse(identifier).orElse(null);
            }
        }

        if (entityId == null) {
            entityId = 0L;
        }

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