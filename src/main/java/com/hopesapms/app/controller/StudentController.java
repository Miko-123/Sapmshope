package com.hopesapms.app.controller;

import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable Integer studentId) {
        StudentResponse response = studentService.getStudentById(studentId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Integer studentId) {
        studentService.deleteStudent(studentId);
        return ResponseEntity.noContent().build();
    }
}