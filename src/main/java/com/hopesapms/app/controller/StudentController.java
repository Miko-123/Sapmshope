package com.hopesapms.app.controller;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.service.ExcelImportService;
import com.hopesapms.app.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Map;


@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ExcelImportService excelImportService;

    @PostMapping("/register")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @Operation(summary = "Register new student", description = "Creates a new student record with pending user status")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Student registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<StudentResponse> registerStudent(@Valid @RequestBody RegisterStudentRequest request) {
        StudentResponse response = studentService.registerStudent(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping(value = "/register/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @Operation(summary = "Bulk register students", description = "Upload an Excel file to register multiple students at once.")
    public ResponseEntity<Map<String, Object>> bulkRegisterStudents(
            @Parameter(description = "Excel file", required = true) @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(excelImportService.importStudents(file));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    @Operation(summary = "Update student details", description = "Update profile, academic status, or assign a section.")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable Integer id,
            @RequestBody UpdateStudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN', 'DEPARTMENT_HEAD', 'INSTRUCTOR')")
    @Operation(summary = "Search for students by name or ID")
    public ResponseEntity<Page<StudentResponse>> searchStudents(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<StudentResponse> students = studentService.searchStudents(query, pageable);
        return ResponseEntity.ok(students);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get all students or Search", description = "Pass 'search' param to filter by name, ID, or email.")
    public ResponseEntity<Page<StudentResponse>> getAllStudents(
            @RequestParam(required = false) String search,
            Pageable pageable) {
        return ResponseEntity.ok(studentService.getAllStudents(search, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Integer id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Integer id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send-verification")
    @PreAuthorize("hasAuthority('STUDENT')")
    public ResponseEntity<String> sendVerification(@RequestParam String email) {
        return ResponseEntity.ok(studentService.sendVerificationCode(email));
    }

    @PostMapping("/verify")
    public ResponseEntity<UserResponseDTO> verifyStudent(
            @RequestParam String email,
            @RequestParam String otp) {
        return ResponseEntity.ok(studentService.verifyStudent(email, otp));
    }
}