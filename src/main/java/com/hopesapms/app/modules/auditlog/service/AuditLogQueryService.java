package com.hopesapms.app.modules.auditlog.service;

import com.hopesapms.app.modules.auditlog.dto.AuditLogResponse;
import com.hopesapms.app.modules.auditlog.model.AuditLog;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.auditlog.repository.AuditLogRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {
    
    private final AuditLogRepository auditLogRepository;

    public Page<AuditLogResponse> getLogs(String search, String actionType, String entityType, Pageable pageable) {
        
        Specification<AuditLog> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchLike = "%" + search.toLowerCase() + "%";
                
                Join<AuditLog, User> userJoin = root.join("user", JoinType.LEFT);

                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(userJoin.get("username")), searchLike),
                    cb.like(cb.lower(root.get("entityType")), searchLike)
                );
                predicates.add(searchPredicate);
            }

            if (StringUtils.hasText(actionType) && !actionType.equals("all")) {
                predicates.add(cb.equal(root.get("actionType"), actionType));
            }

            if (StringUtils.hasText(entityType) && !entityType.equals("all")) {
                predicates.add(cb.equal(root.get("entityType"), entityType));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return auditLogRepository.findAll(spec, pageable)
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