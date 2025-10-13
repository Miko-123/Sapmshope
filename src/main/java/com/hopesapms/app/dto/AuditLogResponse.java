package com.hopesapms.app.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AuditLogResponse {
    private Integer id;
    private String username;
    private String actionType;
    private String entityType;
    private Integer entityId;
    private LocalDateTime timestamp;
    private String oldValue;
    private String newValue;
}
