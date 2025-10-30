package com.hopesapms.app.service;

import com.hopesapms.app.dto.CreateUserRequest;
import com.hopesapms.app.dto.UpdateUserRequest;
import com.hopesapms.app.dto.UserResponse;
import com.hopesapms.app.dto.UpdateUserPrivilegesRequest;
import com.hopesapms.app.model.Role;

import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.RoleRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, String> otpCache = new HashMap<>();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Set<Role> roles = roleRepository.findAllById(request.getRoles())
                .stream()
                .collect(Collectors.toSet());

        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be assigned");
        }

        String baseUsername = (request.getFirstName() + "." + request.getLastName()).toLowerCase()
                .replaceAll("[^a-z0-9.]", "");

        String uniqueUsername = baseUsername;
        int suffix = 1;
        while (userRepository.existsByUsernameAndIsDeletedFalse(uniqueUsername)) {
            uniqueUsername = baseUsername + suffix++;
        }

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName() != null ? request.getMiddleName() : "")
                .lastName(request.getLastName())
                .gender(User.Gender.UNSPECIFIED)
                .roles(roles)
                .username(uniqueUsername)
                .password(null)
                .build();

        User savedUser = userRepository.save(user);

        auditLogService.log("CREATE_USER", "User", savedUser.getId().longValue(), null, savedUser.toString());

        return mapToResponse(savedUser);
    }

    @Transactional
    public UserResponse updateUser(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (!user.getEmail().equals(request.getEmail()) &&
                userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setMiddleName(request.getMiddleName() != null ? request.getMiddleName() : "");
        user.setLastName(request.getLastName());
        user.setGender(request.getGender() != null ? User.Gender.valueOf(request.getGender()) : user.getGender());
        user.setPhoneNumber(request.getPhoneNumber());
        user.setProfilePictureUrl(request.getProfilePictureUrl());

        Set<Role> roles = roleRepository.findAllById(request.getRoleIds())
                .stream()
                .collect(Collectors.toSet());
        if (roles.isEmpty())
            throw new IllegalArgumentException("At least one role must be assigned");
        user.setRoles(roles);

        User updatedUser = userRepository.save(user);
        auditLogService.log("UPDATE_USER", "User", updatedUser.getId().longValue(), null, updatedUser.toString());
        return mapToResponse(updatedUser);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
        auditLogService.log("DELETE", "User", user.getId().longValue(), user.toString(), null);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllActiveUsers(Pageable pageable) {
        return userRepository.findByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getUsersByRole(String roleName, Pageable pageable) {
        return userRepository.findByRoleName(roleName, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public UserResponse updateUserPrivileges(Integer userId, UpdateUserPrivilegesRequest request) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

        String oldRoles = user.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.joining(", "));

        Set<Role> newRoles = roleRepository.findAllById(request.getRoleIds())
        .stream()
        .collect(Collectors.toSet());

        if (newRoles.isEmpty()) {
            throw new IllegalArgumentException("Cannot assign an empty role list. At least one valid role ID must be provided.");
        }

        user.setRoles(newRoles);
        User updatedUser = userRepository.save(user);

        String newRoleStr = updatedUser.getRoles().stream()
        .map(Role::getName)
        .collect(Collectors.joining(", "));

        auditLogService.log("UPDATE_PRIVILEGES" , "User", updatedUser.getId().longValue(), oldRoles, newRoleStr);

        return mapToResponse(updatedUser);
    }

    private UserResponse mapToResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setUsername(user.getUsername());
        r.setEmail(user.getEmail());
        r.setFirstName(user.getFirstName());
        r.setMiddleName(user.getMiddleName());
        r.setLastName(user.getLastName());
        r.setGender(user.getGender() != null ? user.getGender().name() : null);
        r.setPhoneNumber(user.getPhoneNumber());
        r.setProfilePictureUrl(user.getProfilePictureUrl());
        r.setRoles(user.getRoles().stream()
                .map(role -> {
                    UserResponse.RoleResponse rr = new UserResponse.RoleResponse();
                    rr.setId(role.getId());
                    rr.setName(role.getName());
                    return rr;
                })
                .collect(Collectors.toSet()));
        r.setDeleted(user.isDeleted());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedAt(user.getUpdatedAt());
        return r;
    }
}