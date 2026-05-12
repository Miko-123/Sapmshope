package com.hopesapms.app.modules.user.service;

import com.hopesapms.app.modules.auth.dto.ChangePasswordRequest;
import com.hopesapms.app.modules.user.dto.CreateUserRequest;
import com.hopesapms.app.modules.user.dto.UpdateUserRequest;
import com.hopesapms.app.modules.user.dto.UserProfileUpdateRequest;
import com.hopesapms.app.modules.user.dto.UserResponseDTO;
import com.hopesapms.app.modules.user.dto.UpdateUserPrivilegesRequest;
import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.user.model.Role;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.user.repository.RoleRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.auditlog.service.AuditLogService;

@Service
@RequiredArgsConstructor
public class UserService {

        private final AuditLogService auditLogService;
        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;

        private final InstructorRepository instructorRepository;
        private final StudentRepository studentRepository;

        @Transactional
        public UserResponseDTO createUser(CreateUserRequest request) {
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

                boolean isInstructor = roles.stream()
                                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));

                if (isInstructor) {
                        Instructor newInstructor = Instructor.builder()
                                        .user(savedUser)
                                        .build();
                        instructorRepository.save(newInstructor);
                }

                boolean isStudent = roles.stream()
                                .anyMatch(role -> "STUDENT".equals(role.getName()));

                if (isStudent) {

                        Student newStudent = Student.builder()
                                        .user(savedUser)

                                        .build();
                        studentRepository.save(newStudent);
                }

                auditLogService.log("CREATE_USER", "User", savedUser.getId().longValue(), null, savedUser.toString());

                return mapToResponse(savedUser);
        }

        @Transactional
        public UserResponseDTO updateUser(Integer userId, UpdateUserRequest request) {
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
                user.setGender(request.getGender() != null ? User.Gender.valueOf(request.getGender())
                                : user.getGender());
                user.setPassword(request.getPassword());
                user.setPhoneNumber(request.getPhoneNumber());
                user.setProfilePictureUrl(request.getProfilePictureUrl());

                Set<Role> roles = roleRepository.findAllById(request.getRoleIds())
                                .stream()
                                .collect(Collectors.toSet());
                if (roles.isEmpty())
                        throw new IllegalArgumentException("At least one role must be assigned");
                user.setRoles(roles);

                User updatedUser = userRepository.save(user);
                auditLogService.log("UPDATE_USER", "User", updatedUser.getId().longValue(), null,
                                updatedUser.toString());
                return mapToResponse(updatedUser);
        }

        @Transactional
        public UserResponseDTO updateUserProfile(Integer userId, UserProfileUpdateRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));

                if (userRepository.existsByUsernameAndIsDeletedFalseAndIdNot(request.getUsername(), userId)) {
                        throw new IllegalArgumentException("Username already taken");
                }
                user.setUsername(request.getUsername());

                user.setPhoneNumber(request.getPhoneNumber());

                if (request.getGender() != null) {
                        try {
                                user.setGender(User.Gender.valueOf(request.getGender()));
                        } catch (IllegalArgumentException e) {
                                throw new IllegalArgumentException("Invalid gender provided");
                        }
                }

                user.setProfilePictureUrl(request.getProfilePictureUrl());

                User updatedUser = userRepository.save(user);
                auditLogService.log("UPDATE_PROFILE", "User", userId.longValue(), null,
                                "User updated their own profile");

                return mapToResponse(updatedUser);
        }

        @Transactional
        public void changePassword(Integer userId, ChangePasswordRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));

                if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                        throw new IllegalArgumentException("Incorrect current password");
                }

                if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                        throw new IllegalArgumentException("New password and confirm password do not match");
                }

                user.setPassword(passwordEncoder.encode(request.getNewPassword()));
                userRepository.save(user);

                auditLogService.log("CHANGE_PASSWORD", "User", userId.longValue(), null, "User changed password");
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
        public UserResponseDTO getUserById(Integer userId) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found"));
                return mapToResponse(user);
        }

        @Transactional(readOnly = true)
        public Page<UserResponseDTO> getAllActiveUsers(Pageable pageable) {
                return userRepository.findByIsDeletedFalse(pageable).map(this::mapToResponse);
        }

        @Transactional(readOnly = true)
        public Page<UserResponseDTO> getUsersByRole(String roleName, Pageable pageable) {
                return userRepository.findByRoleName(roleName, pageable)
                                .map(this::mapToResponse);
        }

        @Transactional
        public UserResponseDTO updateUserPrivileges(Integer userId, UpdateUserPrivilegesRequest request) {
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + userId));

                String oldRoles = user.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.joining(", "));

                Set<Role> newRoles = roleRepository.findAllById(request.getRoleIds())
                                .stream()
                                .collect(Collectors.toSet());

                if (newRoles.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Cannot assign an empty role list. At least one valid role ID must be provided.");
                }

                user.setRoles(newRoles);
                User updatedUser = userRepository.save(user);

                String newRoleStr = updatedUser.getRoles().stream()
                                .map(Role::getName)
                                .collect(Collectors.joining(", "));

                auditLogService.log("UPDATE_PRIVILEGES", "User", updatedUser.getId().longValue(), oldRoles, newRoleStr);

                return mapToResponse(updatedUser);
        }

        private UserResponseDTO mapToResponse(User user) {
                UserResponseDTO r = new UserResponseDTO();
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
                                        UserResponseDTO.RoleResponse rr = new UserResponseDTO.RoleResponse();
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