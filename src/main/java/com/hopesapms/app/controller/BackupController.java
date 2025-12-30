package com.hopesapms.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/backup")
@RequiredArgsConstructor
public class BackupController {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final CourseRepository courseRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository; 

    @GetMapping("/download")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<byte[]> downloadSystemBackup() throws Exception {
        Map<String, Object> backupData = new HashMap<>();
        
      
        backupData.put("timestamp", LocalDateTime.now().toString());
        backupData.put("version", "1.0");

        backupData.put("users", userRepository.findAll());
        backupData.put("departments", departmentRepository.findAll());
        backupData.put("courses", courseRepository.findAll());
        backupData.put("students", studentRepository.findAll());
        backupData.put("instructors", instructorRepository.findAll());

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule()); 
        
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        byte[] jsonBytes = mapper.writeValueAsBytes(backupData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sapms_backup_" + System.currentTimeMillis() + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBytes);
    }
}