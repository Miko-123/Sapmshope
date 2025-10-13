package com.hopesapms.app.service;

import com.hopesapms.app.dto.RegisterAcademicRequest;
import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.dto.UserResponse;
import com.hopesapms.app.model.Student;
import com.hopesapms.app.repository.DepartmentRepository;
import com.hopesapms.app.repository.ProgramRepository;
import com.hopesapms.app.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final ProgramRepository programRepository;
    private final DepartmentRepository departmentRepository;
    private final UserService userService;
    private final AuditLogService auditLogService;

    @Transactional
    public StudentResponse createStudent(RegisterAcademicRequest request) {
        if (studentRepository.existsByStudentIdAndIsDeletedFalse(request.getStudentId())) {
            throw new IllegalArgumentException("Student ID already exists");
        }

        UserResponse userResponse = userService.createPendingUser(
                request.getEmail(),
                request.getFirstName(),
                request.getMiddleName(),
                request.getLastName()
        );

        Student student = Student.builder()
                .user(userService.getUserEntity(userResponse.getId()))
                .studentId(request.getStudentId())
                .program(programRepository.findById(request.getProgramId())
                        .orElseThrow(() -> new IllegalArgumentException("Program not found")))
                .department(departmentRepository.findById(request.getDepartmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Department not found")))
                .yearLevel(request.getYearLevel())
                .enrollmentDate(request.getEnrollmentDate())
                .status(request.getStatus())
                .build();
        Student savedStudent = studentRepository.save(student);

        auditLogService.log("CREATE_STUDENT", "Student", savedStudent.getId(), null, savedStudent.toString());
        return mapToResponse(savedStudent);
    }

    @Transactional
    public List<StudentResponse> bulkCreateStudents(List<RegisterAcademicRequest> requests) {
        List<String> studentIds = requests.stream().map(RegisterAcademicRequest::getStudentId).collect(Collectors.toList());
        if (!studentRepository.findByStudentIdInAndIsDeletedFalse(studentIds).isEmpty()) {
            throw new IllegalArgumentException("One or more Student IDs already exist");
        }

        List<Student> students = requests.stream().map(request -> {
            UserResponse userResponse = userService.createPendingUser(
                    request.getEmail(),
                    request.getFirstName(),
                    request.getMiddleName(),
                    request.getLastName()
            );
            return Student.builder()
                    .user(userService.getUserEntity(userResponse.getId()))
                    .studentId(request.getStudentId())
                    .program(programRepository.findById(request.getProgramId())
                            .orElseThrow(() -> new IllegalArgumentException("Program not found: " + request.getProgramId())))
                    .department(departmentRepository.findById(request.getDepartmentId())
                            .orElseThrow(() -> new IllegalArgumentException("Department not found: " + request.getDepartmentId())))
                    .yearLevel(request.getYearLevel())
                    .enrollmentDate(request.getEnrollmentDate())
                    .status(request.getStatus())
                    .build();
        }).collect(Collectors.toList());

        List<Student> savedStudents = studentRepository.saveAll(students);
        savedStudents.forEach(student ->
                auditLogService.log("CREATE_STUDENT", "Student", student.getId(), null, student.toString()));
        return savedStudents.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public void deleteStudent(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setDeleted(true);
        studentRepository.save(student);
        userService.deleteUser(student.getUser().getId());
        auditLogService.log("DELETE_STUDENT", "Student", student.getId(), student.toString(), null);
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Integer studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudentsByProgram(Integer programId, Pageable pageable) {
        return studentRepository.findByProgramIdAndIsDeletedFalse(programId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudentsByStatus(String status, Pageable pageable) {
        return studentRepository.findByStatusAndIsDeletedFalse(status, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudentsByEnrollmentDateRange(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return studentRepository.findByEnrollmentDateBetweenAndIsDeletedFalse(startDate, endDate, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getStudentsWithIncompleteProfiles(Pageable pageable) {
        return studentRepository.findStudentsWithIncompleteProfiles(pageable)
                .map(this::mapToResponse);
    }

    private StudentResponse mapToResponse(Student student) {
        StudentResponse response = new StudentResponse();
        response.setId(student.getId());
        response.setStudentId(student.getStudentId());
        response.setProgramId(student.getProgram() != null ? student.getProgram().getId() : null);
        response.setDepartmentId(student.getDepartment() != null ? student.getDepartment().getId() : null);
        response.setYearLevel(student.getYearLevel());
        response.setEnrollmentDate(student.getEnrollmentDate());
        response.setStatus(student.getStatus());
        response.setCreatedAt(student.getCreatedAt());
        response.setUpdatedAt(student.getUpdatedAt());
        if (student.getUser() != null) {
            response.setUserId(student.getUser().getId());
            response.setEmail(student.getUser().getEmail());
            response.setFirstName(student.getUser().getFirstName());
            response.setMiddleName(student.getUser().getMiddleName());
            response.setLastName(student.getUser().getLastName());
            response.setPhoneNumber(student.getUser().getPhoneNumber());
            response.setGender(student.getUser().getGender() != null ? student.getUser().getGender().name() : null);
            response.setProfilePictureUrl(student.getUser().getProfilePictureUrl());
        }
        return response;
    }
}