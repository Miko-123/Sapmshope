package com.hopesapms.app.service;

import com.hopesapms.app.dto.AdminDashboardStatsDTO;
import com.hopesapms.app.dto.AuditLogResponse;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemAdminService {

    private final UserRepository userRepository;
    private final LoginLogRepository loginLogRepository;
    private final AuditLogRepository auditLogRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsDTO getDashboardStats() {

        long totalUsers = userRepository.countActiveUsersWithRoles();
        long totalStudents = studentRepository.countByIsDeletedFalse();
        long totalInstructors = instructorRepository.count();

        long recentAlerts = 5;

        Map<String, Long> distribution = new HashMap<>();
        distribution.put("Students", totalStudents);
        distribution.put("Instructors", totalInstructors);
        distribution.put("Admins", totalUsers - (totalStudents + totalInstructors));

        var recentLogs = auditLogRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "timestamp"))).stream().map(log -> {
                    AuditLogResponse dto = new AuditLogResponse();
                    dto.setId(log.getId());
                    dto.setActionType(log.getActionType());
                    dto.setUsername(log.getUser() != null ? log.getUser().getUsername() : "System");
                    dto.setTimestamp(log.getTimestamp());
                    return dto;
                }).collect(Collectors.toList());

        return AdminDashboardStatsDTO.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalInstructors(totalInstructors)
                .activeUsersNow(12)
                .recentSecurityAlerts(recentAlerts)
                .userRoleDistribution(distribution)
                .recentAuditLogs(recentLogs)
                .build();
    }
}
