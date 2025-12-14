package com.hopesapms.app.service;

import com.hopesapms.app.dto.*;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.context.ApplicationEventPublisher;
import com.hopesapms.app.event.StudentRegisteredEvent;
import com.hopesapms.app.exception.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;
    private final SectionRepository sectionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradingService gradingService;
    private final CourseScheduleRepository courseScheduleRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final Map<String, String> otpCache = new HashMap<>();

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = { IllegalArgumentException.class,
            EntityExistsException.class })
    public StudentResponse registerStudent(RegisterStudentRequest request) {
        if (studentRepository.existsByStudentIdAndIsDeletedFalse(request.getStudentId()))
            throw new IllegalArgumentException("Student ID already exists");

        if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail()))
            throw new IllegalArgumentException("Email already exists");

        Role studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() -> new IllegalArgumentException("STUDENT role not found"));

        User user = User.builder()
                .username((request.getFirstName() + "" + request.getMiddleName()))
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .middleName(request.getMiddleName())
                .lastName(request.getLastName())
                .roles(Set.of(studentRole))
                .build();

        user = userRepository.save(user);

        Department dept = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("Department not found"));
        Program prog = programRepository.findById(request.getProgramId())
                .orElseThrow(() -> new IllegalArgumentException("Program not found"));
        Section section = sectionRepository.findById(request.getSectionId().intValue())
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        Student student = Student.builder()
                .studentId(request.getStudentId())
                .user(user)
                .department(dept)
                .program(prog)
                .section(section)
                .yearLevel(request.getYearLevel())
                .enrollmentDate(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : LocalDate.now())
                .status("PENDING")
                .build();

        Student saved = studentRepository.save(student);
        auditLogService.log("REGISTER_STUDENT", "Student", saved.getId().longValue(), null, student.toString());

        eventPublisher.publishEvent(new StudentRegisteredEvent(this, saved));

        return mapToResponse(saved);
    }

    @Transactional
    public StudentResponse updateStudent(Integer id, UpdateStudentRequest request) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        User user = student.getUser();

        if (request.getFirstName() != null)
            user.setFirstName(request.getFirstName());
        if (request.getMiddleName() != null )
            user.setMiddleName(request.getMiddleName());
        if (request.getLastName() != null)
            user.setLastName(request.getLastName());
        if (request.getEmail() != null && !user.getEmail().equals(request.getEmail())) {
            if (userRepository.existsByEmailAndIsDeletedFalse(request.getEmail()))
                throw new IllegalArgumentException("Email taken");
            user.setEmail(request.getEmail());
        }

        if (request.getYearLevel() != null)
            student.setYearLevel(request.getYearLevel());
        if (request.getStatus() != null)
            student.setStatus(request.getStatus());

        if (request.getProgramId() != null) {
            student.setProgram(programRepository.findById(request.getProgramId())
                    .orElseThrow(() -> new EntityNotFoundException("Program not found")));
        }
        if (request.getSectionId() != null) {
            student.setSection(sectionRepository.findById(request.getSectionId().intValue())
                    .orElseThrow(() -> new EntityNotFoundException("Section not found")));
        }

        userRepository.save(user);
        Student saved = studentRepository.save(student);
        auditLogService.log("UPDATE_STUDENT", "Student", saved.getId().longValue(), null, saved.toString());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> searchStudents(String query, Pageable pageable) {
        Page<Student> students = studentRepository.searchStudents(query, pageable);
        return students.map(this::mapToResponse);
    }

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

    @Transactional(readOnly = true)
    public AcademicHistoryDTO getAcademicHistory(Authentication authentication) {
        Student student = getStudentFromAuth(authentication);

        List<Enrollment> allEnrollments = enrollmentRepository.findByStudent_Id(student.getId().longValue());

        Map<AcademicSemester, List<Enrollment>> enrollmentsBySemester = allEnrollments.stream()
                .filter(e -> e.getCourseOffering().getAcademicSemester() != null)
                .collect(Collectors.groupingBy(e -> e.getCourseOffering().getAcademicSemester()));

        List<AcademicHistoryDTO.SemesterRecordDTO> semesterRecords = new ArrayList<>();
        double totalQualityPoints = 0.0;
        int globalCreditsAttempted = 0;
        int globalCreditsEarned = 0;

        for (Map.Entry<AcademicSemester, List<Enrollment>> entry : enrollmentsBySemester.entrySet()) {
            AcademicSemester semester = entry.getKey();
            List<Enrollment> semesterEnrollments = entry.getValue();

            double semQualityPoints = 0.0;
            int semCreditsAttempted = 0;
            int semCreditsEarned = 0;

            List<StudentCourseDTO> courseDTOs = new ArrayList<>();

            for (Enrollment e : semesterEnrollments) {
                courseDTOs.add(mapToStudentCourseDTO(e));

                if (e.getFinalGrade() != null) {
                    double credits = e.getCourseOffering().getCourse().getCredits();
                    Double gradePoints = null;

                    try {
                        double score = Double.parseDouble(e.getFinalGrade());
                        GradingScale gs = gradingService.calculateGrade(score);
                        if (gs != null)
                            gradePoints = gs.getGradePoint();
                    } catch (NumberFormatException ex) {
                        gradePoints = gradingService.getPointsForLetter(e.getFinalGrade());
                    }

                    if (gradePoints != null) {
                        semCreditsAttempted += credits;
                        semQualityPoints += (gradePoints * credits);

                        if (gradePoints > 0) {
                            semCreditsEarned += credits;
                        }
                    }
                }
            }

            double semesterGPA = (semCreditsAttempted > 0) ? (semQualityPoints / semCreditsAttempted) : 0.0;

            totalQualityPoints += semQualityPoints;
            globalCreditsAttempted += semCreditsAttempted;
            globalCreditsEarned += semCreditsEarned;

            semesterRecords.add(AcademicHistoryDTO.SemesterRecordDTO.builder()
                    .semesterId(semester.getId())
                    .semesterName(semester.getName())
                    .year(semester.getYear())
                    .status(semester.getStatus())
                    .semesterGPA(semesterGPA)
                    .semesterCredits(semCreditsEarned)
                    .courses(courseDTOs)
                    .build());
        }

        semesterRecords.sort((a, b) -> {
            if ("ACTIVE".equals(a.getStatus()))
                return -1;
            if ("ACTIVE".equals(b.getStatus()))
                return 1;
            return Long.compare(b.getSemesterId(), a.getSemesterId());
        });

        double cgpa = (globalCreditsAttempted > 0) ? (totalQualityPoints / globalCreditsAttempted) : 0.0;

        return AcademicHistoryDTO.builder()
                .studentName(student.getUser().getFirstName() + " " + student.getUser().getLastName())
                .studentId(student.getStudentId())
                .cumulativeGPA(cgpa)
                .totalCreditsEarned(globalCreditsEarned)
                .semesters(semesterRecords)
                .build();
    }

    @Transactional
    public int promoteStudents(BatchPromoteRequest request) {

        List<Student> students = studentRepository.findByProgram_IdAndYearLevelAndIsDeletedFalse(
                request.getProgramId(),
                request.getCurrentYearLevel());

        if (students.isEmpty()) {
            return 0;
        }

        for (Student student : students) {
            if (request.isGraduating()) {
                student.setStatus("GRADUATED");

            } else {
                student.setYearLevel(request.getNextYearLevel());
            }
        }

        studentRepository.saveAll(students);

        auditLogService.log("BATCH_PROMOTE", "Student", 0L,
                "Program: " + request.getProgramId(),
                "Promoted " + students.size() + " students from Year " + request.getCurrentYearLevel());

        return students.size();
    }

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

    @Transactional(readOnly = true)
    public List<StudentCourseDTO> getMyActiveCourses(Authentication authentication) {
        Student student = getStudentFromAuth(authentication);
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdAndStatus(student.getId().longValue(),
                "ENROLLED");
        return enrollments.stream()
                .filter(e -> {
                    AcademicSemester sem = e.getCourseOffering().getAcademicSemester();
                    return sem != null && Boolean.TRUE.equals(sem.isCurrent());
                })
                .map(this::mapToStudentCourseDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudentCourseDetailDTO getCourseDetails(Long enrollmentId, Authentication authentication) {
        Student student = getStudentFromAuth(authentication);

        Enrollment enrollment = enrollmentRepository.findById(enrollmentId.intValue())
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        if (!enrollment.getStudent().getId().equals(student.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Unauthorized access to this enrollment.");
        }

        StudentCourseDetailDTO dto = new StudentCourseDetailDTO();
        dto.setEnrollmentId(enrollment.getId().longValue());

        CourseOffering offering = enrollment.getCourseOffering();
        dto.setCourseTitle(offering.getCourse().getTitle());
        dto.setCourseCode(offering.getCourse().getCourseCode());

        if (offering.getInstructor() != null && offering.getInstructor().getUser() != null) {
            User u = offering.getInstructor().getUser();
            dto.setInstructorName(u.getFirstName() + " " + u.getLastName());
        } else {
            dto.setInstructorName("TBD");
        }

        List<Assessment> courseAssessments = assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(
                offering.getId());

        double totalScore = 0.0;

        for (Assessment assessment : courseAssessments) {
            StudentCourseDetailDTO.StudentAssessmentDTO aDto = new StudentCourseDetailDTO.StudentAssessmentDTO();
            aDto.setAssessmentName(assessment.getName());
            aDto.setType(assessment.getType());
            aDto.setMaxScore(assessment.getMaxScore().doubleValue());
            aDto.setWeight(assessment.getWeight().doubleValue());

            Optional<Score> scoreOpt = scoreRepository.findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(
                    enrollment.getId().intValue(),
                    assessment.getId());

            if (scoreOpt.isPresent()) {
                aDto.setScored(scoreOpt.get().getScoreValue().doubleValue());
                aDto.setStatus("GRADED");
                totalScore += aDto.getScored();
            } else {
                aDto.setScored(null);
                aDto.setStatus("PENDING");
            }

            dto.getAssessments().add(aDto);
        }

        dto.setTotalGrade(totalScore);

        if (totalScore > 0) {
            GradingScale grade = gradingService.calculateGrade(totalScore);
            if (grade != null) {
                dto.setLetterGrade(grade.getLetterGrade());
                dto.setGradePoint(grade.getGradePoint());
            }
        }

        List<Attendance> attendanceList = attendanceRepository.findByStudentAndCourse(
                student.getId(),
                offering.getId());

        for (Attendance att : attendanceList) {
            StudentCourseDetailDTO.StudentAttendanceDTO attDto = new StudentCourseDetailDTO.StudentAttendanceDTO();

            if (att.getClassSession() != null && att.getClassSession().getSessionDate() != null) {
                attDto.setDate(att.getClassSession().getSessionDate().toString());
            } else {
                attDto.setDate("Unknown Date");
            }
            attDto.setStatus(att.getStatus());
            attDto.setRemarks(att.getRemarks());
            dto.getAttendance().add(attDto);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public List<StudentScheduleDTO> getWeeklySchedule(Authentication authentication) {
        Student student = getStudentFromAuth(authentication);
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_IdAndStatus(student.getId().longValue(),
                "ENROLLED");

        List<StudentScheduleDTO> scheduleList = new ArrayList<>();

        for (Enrollment e : enrollments) {
            List<CourseSchedule> courseSchedules = courseScheduleRepository
                    .findByCourseOffering_Id(e.getCourseOffering().getId());

            for (CourseSchedule cs : courseSchedules) {
                StudentScheduleDTO dto = new StudentScheduleDTO();
                dto.setDay(cs.getDay());
                dto.setCourseCode(e.getCourseOffering().getCourse().getCourseCode());
                dto.setCourseTitle(e.getCourseOffering().getCourse().getTitle());
                dto.setRoom(cs.getRoom());
                dto.setTime(convertPeriodsToTimeDisplay(cs.getPeriods()));

                if (e.getCourseOffering().getInstructor() != null) {
                    User u = e.getCourseOffering().getInstructor().getUser();
                    dto.setInstructor(u.getFirstName() + " " + u.getLastName());
                } else {
                    dto.setInstructor("Staff");
                }

                scheduleList.add(dto);
            }
        }
        return scheduleList;
    }

    private String convertPeriodsToTimeDisplay(String periods) {
        if (periods == null)
            return "TBD";
        if (periods.contains("1") || periods.contains("2"))
            return "08:30 - 10:20";
        if (periods.contains("3") || periods.contains("4"))
            return "10:30 - 12:20";
        if (periods.contains("5") || periods.contains("6"))
            return "13:30 - 15:20";
        if (periods.contains("7") || periods.contains("8"))
            return "15:30 - 17:20";
        return periods;
    }

    public Student getStudentFromAuth(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return studentRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public StudentDashboardStatsDTO getDashboardStats(Authentication authentication) {
        Student student = getStudentFromAuth(authentication);

        List<Enrollment> allEnrollments = enrollmentRepository.findByStudent_Id(student.getId().longValue());

        double totalPoints = 0.0;
        int totalGradedCredits = 0;

        double semesterPoints = 0.0;
        int semesterGradedCredits = 0;

        int totalAttemptedCredits = 0;
        int semesterAttemptedCredits = 0;

        for (Enrollment e : allEnrollments) {

            if (!"ENROLLED".equals(e.getStatus()) && !"COMPLETED".equals(e.getStatus())) {
                continue;
            }

            double credits = e.getCourseOffering().getCourse().getCredits();

            totalAttemptedCredits += credits;

            if ("ENROLLED".equals(e.getStatus())) {
                semesterAttemptedCredits += credits;
            }

            Double gradePoints = null;

            if (e.getFinalGrade() != null) {
                try {
                    double score = Double.parseDouble(e.getFinalGrade());
                    GradingScale gs = gradingService.calculateGrade(score);
                    if (gs != null)
                        gradePoints = gs.getGradePoint();
                } catch (NumberFormatException ex) {
                    gradePoints = gradingService.getPointsForLetter(e.getFinalGrade());
                }
            }

            else {

                List<Assessment> assessments = assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(
                        e.getCourseOffering().getId());

                double calculatedTotal = 0.0;
                boolean hasScores = false;

                for (Assessment a : assessments) {
                    Optional<Score> scoreOpt = scoreRepository.findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(
                            e.getId().intValue(), a.getId());
                    if (scoreOpt.isPresent()) {
                        calculatedTotal += scoreOpt.get().getScoreValue().doubleValue();
                        hasScores = true;
                    }
                }

                if (hasScores) {
                    GradingScale gs = gradingService.calculateGrade(calculatedTotal);
                    if (gs != null)
                        gradePoints = gs.getGradePoint();
                }
            }

            if (gradePoints != null) {
                double points = gradePoints * credits;

                totalPoints += points;
                totalGradedCredits += credits;

                if ("ENROLLED".equals(e.getStatus()) || "COMPLETED".equals(e.getStatus())) {
                    semesterPoints += points;
                    semesterGradedCredits += credits;
                }
            }

        }

        StudentDashboardStatsDTO stats = new StudentDashboardStatsDTO();

        if (totalGradedCredits > 0) {
            stats.setCumulativeGPA(totalPoints / totalGradedCredits);
        } else {
            stats.setCumulativeGPA(0.0);
        }

        if (semesterGradedCredits > 0) {
            stats.setSemesterGPA(semesterPoints / semesterGradedCredits);
        } else {
            stats.setSemesterGPA(0.0);
        }

        stats.setTotalCreditsEarned(totalAttemptedCredits);
        stats.setCurrentSemesterCredits(semesterAttemptedCredits);

        return stats;
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        return mapToResponse(student);
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> getAllStudents(String search, Pageable pageable) {
        if (search != null && !search.isBlank()) {
            return studentRepository.searchStudents(search, pageable).map(this::mapToResponse);
        }
        return studentRepository.findAllByIsDeletedFalse(pageable).map(this::mapToResponse);
    }

    @Transactional
    public void deleteStudent(Integer id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        student.setDeleted(true);
        studentRepository.save(student);
        auditLogService.log("DELETE_STUDENT", "Student", student.getId().longValue(), student.toString(), null);
    }

    private StudentCourseDTO mapToStudentCourseDTO(Enrollment e) {
        StudentCourseDTO dto = new StudentCourseDTO();
        dto.setEnrollmentId(e.getId().longValue());

        CourseOffering offering = e.getCourseOffering();
        dto.setCourseOfferingId(offering.getId());
        dto.setCourseCode(offering.getCourse().getCourseCode());
        dto.setCourseTitle(offering.getCourse().getTitle());
        dto.setSectionName(offering.getSection().getName());
        dto.setContactHours(offering.getContactHours());
        dto.setSemesterName(offering.getAcademicSemester().getName());
        dto.setStatus(e.getStatus());

        if (offering.getInstructor() != null && offering.getInstructor().getUser() != null) {
            User instUser = offering.getInstructor().getUser();
            dto.setInstructorName(instUser.getFirstName() + " " + instUser.getLastName());
        } else {
            dto.setInstructorName("TBD");
        }

        if (e.getFinalGrade() != null) {
            try {
                dto.setCurrentGrade(Double.parseDouble(e.getFinalGrade()));
            } catch (NumberFormatException ex) {
                dto.setLetterGrade(e.getFinalGrade());
            }
        }

        return dto;
    }

    private StudentResponse mapToResponse(Student s) {
        StudentResponse r = new StudentResponse();
        r.setId(s.getId());

        if (s.getUser() != null) {
            r.setUserId(s.getUser().getId());
            r.setEmail(s.getUser().getEmail());
            r.setFirstName(s.getUser().getFirstName());
            r.setMiddleName(s.getUser().getMiddleName());
            r.setLastName(s.getUser().getLastName());
            r.setPhoneNumber(s.getUser().getPhoneNumber());
            if (s.getUser().getGender() != null) {
                r.setGender(s.getUser().getGender().name());
            }
        }

        r.setStudentId(s.getStudentId());
        r.setStatus(s.getStatus());
        r.setYearLevel(s.getYearLevel());
        r.setEnrollmentDate(s.getEnrollmentDate());
        r.setCreatedAt(s.getCreatedAt());
        r.setUpdatedAt(s.getUpdatedAt());

        if (s.getDepartment() != null) {
            r.setDepartmentId(s.getDepartment().getId());
            r.setDepartmentName(s.getDepartment().getName());
        }
        if (s.getProgram() != null) {
            r.setProgramId(s.getProgram().getId());
            r.setProgramName(s.getProgram().getName());
        }
        if (s.getSection() != null) {
            r.setSectionId(s.getSection().getId());
            r.setSectionName(s.getSection().getName());
        }
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