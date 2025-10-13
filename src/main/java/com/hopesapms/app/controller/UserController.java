package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CompleteProfileRequest;
import com.hopesapms.app.dto.CreateUserRequest;
import com.hopesapms.app.dto.RegisterAcademicRequest;
import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.dto.UpdateUserRequest;
import com.hopesapms.app.dto.UserResponse;
import com.hopesapms.app.service.StudentService;
import com.hopesapms.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users and students in SAPMS")
public class UserController {

    private final UserService userService;
    private final StudentService studentService;

    @GetMapping("/test")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Test System Admin access", description = "Checks if the user has SYSTEM_ADMIN authority")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Access granted", content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority")
    })
    public ResponseEntity<String> testAdminAccess() {
        return ResponseEntity.ok("Access granted");
    }

    @PostMapping("/register-academic")
    @PreAuthorize("hasAnyAuthority('SYSTEM_ADMIN', 'REGISTRAR')")
    @Operation(summary = "Register a new student", description = "Creates a student record and associated user (pending activation)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Student created", content = @Content(schema = @Schema(implementation = StudentResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<StudentResponse> registerAcademic(@Valid @RequestBody RegisterAcademicRequest request) {
        StudentResponse response = studentService.createStudent(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/students/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/send-verification")
    @Operation(summary = "Send verification code", description = "Sends an OTP to the user's email for account verification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Verification code sent", content = @Content(mediaType = "text/plain")),
        @ApiResponse(responseCode = "400", description = "Invalid email or user already activated")
    })
    public ResponseEntity<String> sendVerificationCode(
            @Parameter(description = "Email address to send OTP to") @RequestParam String email) {
        try {
            String message = userService.sendVerificationCode(email);
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify user account", description = "Verifies the OTP sent to the user's email")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Account verified", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid or expired OTP", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    public ResponseEntity<UserResponse> verifyAccount(
            @Parameter(description = "Email address of the user") @RequestParam String email,
            @Parameter(description = "OTP code") @RequestParam String otp) {
        try {
            UserResponse response = userService.verifyAndActivate(email, otp);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            UserResponse errorResponse = new UserResponse();
            errorResponse.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/complete-profile")
    @Operation(summary = "Complete user profile", description = "Allows a user to complete their profile after OTP verification")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile completed", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content(schema = @Schema(implementation = UserResponse.class)))
    })
    public ResponseEntity<UserResponse> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        try {
            UserResponse response = userService.completeProfile(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            UserResponse errorResponse = new UserResponse();
            errorResponse.setErrorMessage(e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new user", description = "Creates a new user with specified roles (SYSTEM_ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse response = userService.createUser(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Update a user", description = "Updates user details (SYSTEM_ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "ID of the user to update") @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Delete a user", description = "Soft deletes a user by ID (SYSTEM_ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID of the user to delete") @PathVariable Integer userId) {
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Get user by ID", description = "Retrieves a user by ID (SYSTEM_ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "ID of the user to retrieve") @PathVariable Integer userId) {
        UserResponse response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("all-users")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Get all active users", description = "Retrieves a paginated list of active users (SYSTEM_ADMIN only)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of users", content = @Content(schema = @Schema(implementation = Page.class))),
        @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority")
    })
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllActiveUsers(pageable));
    }
}