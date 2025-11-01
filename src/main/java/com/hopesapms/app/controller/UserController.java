package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CompleteProfileRequest;
import com.hopesapms.app.dto.UpdateUserPrivilegesRequest;
import com.hopesapms.app.dto.CreateUserRequest;
import com.hopesapms.app.dto.UpdateUserRequest;
import com.hopesapms.app.dto.UserResponseDTO;
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

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Create a new user", description = "Creates a new user with specified roles (SYSTEM_ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority")
    })
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponseDTO response = userService.createUser(request);
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
            @ApiResponse(responseCode = "200", description = "User updated", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> updateUser(
            @Parameter(description = "ID of the user to update") @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserRequest request) {
        UserResponseDTO response = userService.updateUser(userId, request);
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
            @ApiResponse(responseCode = "200", description = "User found", content = @Content(schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "ID of the user to retrieve") @PathVariable Integer userId) {
        UserResponseDTO response = userService.getUserById(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("all-users")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Get all active users", description = "Retrieves a paginated list of active users (SYSTEM_ADMIN only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of users", content = @Content(schema = @Schema(implementation = Page.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority")
    })
    public ResponseEntity<Page<UserResponseDTO>> getAllUsers(
            @Parameter(description = "Pagination and sorting parameters") Pageable pageable) {
        return ResponseEntity.ok(userService.getAllActiveUsers(pageable));
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "List active users", description = "Retrieves users by their assigned role")
    public ResponseEntity<Page<UserResponseDTO>> getUserByRole(
            @PathVariable String roleName,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getUsersByRole(roleName, pageable));

    }

    @PutMapping("/{userId}/privileges")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    @Operation(summary = "Manage user privileges (UC-003)", description = "Updates *only* the roles for a specific user. This fulfills UC-003.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Privileges updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data (e.g., empty role list)"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Requires SYSTEM_ADMIN authority"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserResponseDTO> updateUserPrivileges(
            @Parameter(description = "ID of the user to update") @PathVariable Integer userId,
            @Valid @RequestBody UpdateUserPrivilegesRequest request) {
        UserResponseDTO response = userService.updateUserPrivileges(userId, request);
        return ResponseEntity.ok(response);
    }

}