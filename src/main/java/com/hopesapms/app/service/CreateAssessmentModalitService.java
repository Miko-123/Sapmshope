package com.hopesapms.app.service;

import org.springframework.stereotype.Service;

import com.hopesapms.app.dto.CreateAssessmentsModalityDTO;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAssessmentModalitService {
    private final AuditLogService auditLogService;

    @Transactional
    public void createModality(CreateAssessmentsModalityDTO requestDto) {
        
    }
}
