package com.hopesapms.app.controller;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // ✅ 1. Registrar registers a student
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

    // ✅ 2. Send OTP verification (Registrar-triggered or system-triggered)
    @PostMapping("/send-verification")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public ResponseEntity<String> sendVerification(@RequestParam String email) {
        return ResponseEntity.ok(studentService.sendVerificationCode(email));
    }

    // ✅ 3. Student verifies their account
    @PostMapping("/verify")
    public ResponseEntity<UserResponse> verifyStudent(
            @RequestParam String email,
            @RequestParam String otp) {
        return ResponseEntity.ok(studentService.verifyStudent(email, otp));
    }

   /*  // ✅ 4. Student completes their profile
    @PostMapping("/complete-profile")
    public ResponseEntity<UserResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        return ResponseEntity.ok(studentService.completeStudentProfile(request));
    }*/

    // ✅ 5. Get student by ID (Registrar / Admin)
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    public ResponseEntity<StudentResponse> getStudent(@PathVariable Integer id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    // ✅ 6. List all students (Registrar)
    @GetMapping
    @PreAuthorize("hasAnyAuthority('REGISTRAR', 'SYSTEM_ADMIN')")
    public ResponseEntity<Page<StudentResponse>> getAllStudents(Pageable pageable) {
        return ResponseEntity.ok(studentService.getAllStudents(pageable));
    }

    // ✅ 7. Delete student
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('REGISTRAR')")
    public ResponseEntity<Void> deleteStudent(@PathVariable Integer id) {
        studentService.deleteStudent(id);
        return ResponseEntity.noContent().build();
    }
}
