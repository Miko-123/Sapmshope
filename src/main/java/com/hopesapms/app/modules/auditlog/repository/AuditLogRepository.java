package com.hopesapms.app.modules.auditlog.repository;

import com.hopesapms.app.modules.auditlog.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    Long countByActionType(String actionType);

    Page<AuditLog> findByUser_Username(String username, Pageable pageable);
}