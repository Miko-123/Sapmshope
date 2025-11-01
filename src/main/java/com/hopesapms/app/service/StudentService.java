package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;

    private final Map<String, String> otpCache = new HashMap<>();

    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,}$");
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[1-9]\\d{1,14}$");

    // 1️⃣ Register new student (Registrar)
    @Transactional
    public StudentResponse registerStudent(RegisterStudentRequest request) {
        if (studentRepository.existsByStudentIdAndIsDeletedFalse(request.getStudentId()))
            throw new IllegalArgumentException("Student ID already exists");

        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail()))
            throw new IllegalArgumentException("Email already exists");

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalArgumentException("STUDENT role not found"));

        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .roles(Set.of(studentRole))
                .build();

        user = userRepository.save(user);

        Student student = Student.builder()
                .studentId(request.getStudentId())
                .user(user)
                .department(departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found")))
                .program(programRepository.findById(request.getProgramId())
                        .orElseThrow(() -> new IllegalArgumentException("Program not found")))
                .yearLevel(request.getYearLevel())
                .status("PENDING")
                .build();

        student = studentRepository.save(student);
        auditLogService.log("REGISTER_STUDENT", "Student", student.getId().longValue(), null, student.toString());
        return mapToResponse(student);
    }

    // 2️⃣ Send OTP verification
    public String sendVerificationCode(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new IllegalArgumentException("Email not found"));

        if (user.getPassword() != null)
            throw new IllegalArgumentException("User already activated");

        String otp = String.format("%06d", new Random().nextInt(999999));
        otpCache.put(email, otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("SAPMS Verification Code");
        message.setText("Your verification code is: " + otp + ". Expires in 15 minutes.");
        mailSender.send(message);

        return "Verification code sent successfully";
    }

    // 3️⃣ Verify OTP
    @Transactional
    public UserResponseDTO verifyStudent(String email, String otp) {
        String cachedOtp = otpCache.get(email);
        if (cachedOtp == null || !cachedOtp.equals(otp))
            throw new IllegalArgumentException("Invalid or expired verification code");

        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        otpCache.remove(email);
        auditLogService.log("VERIFY_STUDENT", "User", user.getId().longValue(), null, user.toString());
        return mapToUserResponse(user);
    }

    /*@Transactional
    public UserResponse completeStudentProfile(CompleteProfileRequest request) {
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getPassword() != null)
            throw new IllegalArgumentException("Profile already completed");

        if (userRepository.existsByUsernameAndIsDeletedFalse(request.getUsername()))
            throw new IllegalArgumentException("Username already taken");

        if (!PASSWORD_PATTERN.matcher(request.getPassword()).matches())
            throw new IllegalArgumentException("Password must be at least 8 characters with letters and numbers");

        if (request.getPhoneNumber() != null && !PHONE_PATTERN.matcher(request.getPhoneNumber()).matches())
            throw new IllegalArgumentException("Invalid phone number format");

        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        if (request.getGender() != null)
            user.setGender(User.Gender.valueOf(request.getGender()));

        user = userRepository.save(user);
        auditLogService.log("COMPLETE_PROFILE", "User", user.getId(), null, user.toString());
        return mapToUserResponse(user);
    }
*/
    // 5️⃣ Get student by ID
    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        return mapToResponse(student);
    }

    // 6️⃣ Get all students
    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(Pageable pageable) {
        return studentRepository.findAllByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    // 7️⃣ Delete student
    @Transactional
    public void deleteStudent(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setDeleted(true);
        studentRepository.save(student);
        auditLogService.log("DELETE_STUDENT", "Student", student.getId().longValue(), student.toString(), null);
    }

    // 🧭 Helper mappers
    private StudentResponse mapToResponse(Student s) {
        StudentResponse r = new StudentResponse();
        r.setId(s.getId());
        r.setStudentId(s.getStudentId());
        r.setEmail(s.getUser().getEmail());
        r.setFirstName(s.getUser().getFirstName());
        r.setMiddleName(s.getUser().getMiddleName());
        r.setLastName(s.getUser().getLastName());
        r.setStatus(s.getStatus());
        r.setDepartmentId(s.getDepartment().getId());
        r.setProgramId(s.getProgram().getId());
        return r;
    }

    private UserResponseDTO mapToUserResponse(User u) {
        UserResponseDTO r = new UserResponseDTO();
        r.setId(u.getId());
        r.setUsername(u.getUsername());
        r.setEmail(u.getEmail());
        r.setFirstName(u.getFirstName());
        r.setLastName(u.getLastName());
        r.setRoles(u.getRoles().stream()
                .map(role -> {
                    UserResponseDTO.RoleResponse rr = new UserResponseDTO.RoleResponse();
                    rr.setId(role.getId());
                    rr.setName(role.getName());
                    return rr;
                })
                .collect(Collectors.toSet()));
        return r;
    }
}
