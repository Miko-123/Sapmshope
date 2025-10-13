package com.hopesapms.app.service;

import com.hopesapms.app.dto.CompleteProfileRequest;
import com.hopesapms.app.dto.CreateUserRequest;
import com.hopesapms.app.dto.UpdateUserRequest;
import com.hopesapms.app.dto.UserResponse;
import com.hopesapms.app.model.Role;
import com.hopesapms.app.model.Student;
import com.hopesapms.app.model.User;
import com.hopesapms.app.repository.RoleRepository;
import com.hopesapms.app.repository.StudentRepository;
import com.hopesapms.app.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    private final Map<String, String> otpCache = new HashMap<>();
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");

    @Transactional
    public UserResponse createPendingUser(String email, String firstName, String middleName, String lastName) {
        if (userRepository.existsByEmailAndIsDeletedFalse(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        User user = User.builder()
                .email(email)
                .firstName(firstName)
                .middleName(middleName != null ? middleName : "")
                .lastName(lastName)
                .gender(User.Gender.MALE)
                .roles(Set.of(roleRepository.findByName("STUDENT")
                        .orElseThrow(() -> new IllegalArgumentException("STUDENT role not found"))))
                .build();
        User savedUser = userRepository.save(user);
        auditLogService.log("CREATE_PENDING_USER", "User", savedUser.getId(), null, savedUser.toString());
        return mapToResponse(savedUser, null);
    }

    @Transactional
    public String sendVerificationCode(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));
        if (user.getPassword() != null) {
            throw new IllegalArgumentException("User already activated");
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(email, otp);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("SAPMS Account Verification");
        message.setText("Your verification code is: " + otp + ". Expires in 15 minutes.");
        mailSender.send(message);
        return "Verification code sent";
    }

    @Transactional
    public UserResponse verifyAndActivate(String email, String otp) {
        String storedOtp = otpCache.get(email);
        if (storedOtp == null || !storedOtp.equals(otp)) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        Student student = studentRepository.findByUserId(user.getId()).orElse(null);
        otpCache.remove(email);
        auditLogService.log("VERIFY", "User", user.getId(), null, user.toString());
        return mapToResponse(user, student);
    }

    @Transactional
    public UserResponse completeProfile(CompleteProfileRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + request.getEmail()));
        if (user.getPassword() != null) {
            throw new IllegalArgumentException("Profile already completed");
        }
        if (userRepository.existsByUsernameAndIsDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already taken");
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters with letters and numbers");
        }
        if (request.getPhoneNumber() != null && !PHONE_PATTERN.matcher(request.getPhoneNumber()).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null) {
            user.setGender(User.Gender.valueOf(request.getGender()));
        }
        User savedUser = userRepository.save(user);
        Student student = studentRepository.findByUserId(savedUser.getId()).orElse(null);
        auditLogService.log("COMPLETE_PROFILE", "User", savedUser.getId(), null, savedUser.toString());
        return mapToResponse(savedUser, student);
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsernameAndIsDeletedFalse(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches()) {
            throw new IllegalArgumentException("Password must be at least 8 characters with letters and numbers");
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName() != null ? request.getMiddleName() : "")
                .lastName(request.getLastName())
                .gender(request.getGender() != null ? User.Gender.valueOf(request.getGender()) : User.Gender.MALE)
                .phoneNumber(request.getPhoneNumber())
                .profilePictureUrl(request.getProfilePictureUrl())
                .build();

        Set<Role> roles = roleRepository.findAllById(request.getRoles())
                .stream()
                .collect(Collectors.toSet());
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be assigned");
        }
        user.setRoles(roles);

        User savedUser = userRepository.save(user);
        auditLogService.log("CREATE", "User", savedUser.getId(), null, savedUser.toString());
        return mapToResponse(savedUser, null);
    }

    @Transactional
    public UserResponse updateUser(Integer userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.getEmail().equals(request.getEmail()) && userRepository.existsByEmailAndIsDeletedFalse(request.getEmail())) {
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
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("At least one role must be assigned");
        }
        user.setRoles(roles);

        User updatedUser = userRepository.save(user);
        Student student = studentRepository.findByUserId(userId).orElse(null);
        auditLogService.log("UPDATE", "User", updatedUser.getId(), null, updatedUser.toString());
        return mapToResponse(updatedUser, student);
    }

    @Transactional
    public void deleteUser(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setDeleted(true);
        userRepository.save(user);
        auditLogService.log("DELETE", "User", user.getId(), user.toString(), null);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Student student = studentRepository.findByUserId(userId).orElse(null);
        return mapToResponse(user, student);
    }

    @Transactional(readOnly = true)
    public User getUserEntity(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllActiveUsers(Pageable pageable) {
        return userRepository.findByIsDeletedFalse(pageable)
                .map(user -> {
                    Student student = studentRepository.findByUserId(user.getId()).orElse(null);
                    return mapToResponse(user, student);
                });
    }

    private UserResponse mapToResponse(User user, Student student) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setMiddleName(user.getMiddleName());
        response.setLastName(user.getLastName());
        response.setGender(user.getGender() != null ? user.getGender().name() : null);
        response.setPhoneNumber(user.getPhoneNumber());
        response.setProfilePictureUrl(user.getProfilePictureUrl());
        response.setDeleted(user.isDeleted());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setRoles(user.getRoles().stream()
                .map(role -> {
                    UserResponse.RoleResponse roleResponse = new UserResponse.RoleResponse();
                    roleResponse.setId(role.getId());
                    roleResponse.setName(role.getName());
                    return roleResponse;
                })
                .collect(Collectors.toSet()));
        if (student != null) {
            response.setStudentId(student.getStudentId());
            response.setProgramId(student.getProgram() != null ? student.getProgram().getId() : null);
            response.setDepartmentId(student.getDepartment() != null ? student.getDepartment().getId() : null);
            response.setYearLevel(student.getYearLevel());
            response.setEnrollmentDate(student.getEnrollmentDate());
            response.setStatus(student.getStatus());
        }
        return response;
    }
}