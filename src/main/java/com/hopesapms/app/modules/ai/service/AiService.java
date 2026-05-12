package com.hopesapms.app.modules.ai.service;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.hopesapms.app.modules.ai.dto.AiRequestDTO;
import com.hopesapms.app.modules.academicsemester.model.AcademicSemester;
import com.hopesapms.app.modules.attendance.model.Attendance;
import com.hopesapms.app.modules.course.model.Course;
import com.hopesapms.app.modules.courseoffering.model.CourseOffering;
import com.hopesapms.app.modules.schedule.model.CourseSchedule;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.grading.model.GradingScale;
import com.hopesapms.app.modules.instructor.model.Instructor;
import com.hopesapms.app.modules.program.model.Program;
import com.hopesapms.app.modules.user.model.Role;
import com.hopesapms.app.modules.room.model.Room;
import com.hopesapms.app.modules.score.model.Score;
import com.hopesapms.app.modules.section.model.Section;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.academicsemester.repository.AcademicSemesterRepository;
import com.hopesapms.app.modules.assessment.repository.AssessmentRepository;
import com.hopesapms.app.modules.attendance.repository.AttendanceRepository;
import com.hopesapms.app.modules.auditlog.repository.AuditLogRepository;
import com.hopesapms.app.modules.courseoffering.repository.CourseOfferingRepository;
import com.hopesapms.app.modules.course.repository.CourseRepository;
import com.hopesapms.app.modules.schedule.repository.CourseScheduleRepository;
import com.hopesapms.app.modules.department.repository.DepartmentRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import com.hopesapms.app.modules.attendance.repository.ExcusedAbsenceRequestRepository;
import com.hopesapms.app.modules.grading.repository.GradeChangeRequestRepository;
import com.hopesapms.app.modules.grading.repository.GradingScaleRepository;
import com.hopesapms.app.modules.instructor.repository.InstructorRepository;
import com.hopesapms.app.modules.notification.repository.NotificationRepository;
import com.hopesapms.app.modules.course.repository.PrerequisiteRepository;
import com.hopesapms.app.modules.program.repository.ProgramRepository;
import com.hopesapms.app.modules.room.repository.RoomRepository;
import com.hopesapms.app.modules.score.repository.ScoreRepository;
import com.hopesapms.app.modules.section.repository.SectionRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    // --- 1. CORE DATA (Users & Structure) ---
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final InstructorRepository instructorRepository;
    private final DepartmentRepository departmentRepository;
    private final ProgramRepository programRepository;
    private final SectionRepository sectionRepository;

    // --- 2. ACADEMIC DATA (Courses & Enrollment) ---
    private final AcademicSemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final PrerequisiteRepository prerequisiteRepository; // NEW: Context for advising

    // --- 3. PERFORMANCE DATA (Grades & Attendance) ---
    private final ScoreRepository scoreRepository;
    private final AssessmentRepository assessmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final GradingScaleRepository gradingScaleRepository; // NEW: Context for "What is an A?"
    private final GradeChangeRequestRepository gradeChangeRepository; // NEW: Context for disputes
    private final ExcusedAbsenceRequestRepository excusedAbsenceRepository; // NEW: Context for sick leave

    // --- 4. SYSTEM DATA (Logs & Alerts) ---
    private final AuditLogRepository auditLogRepository;
    private final NotificationRepository notificationRepository; // NEW: Context for alerts
    private final RoomRepository roomRepository; // NEW: Context for scheduling

    // --- Scheduling ---
    private final CourseScheduleRepository scheduleRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Transactional(readOnly = true)
    public String generateContent(AiRequestDTO request) {
        try {
            String systemData = "";
            String aiPersona = "";

            switch (request.getContextType()) {
               
                case "SCHEDULING_ASSIST":
                    aiPersona = "ROLE: You are an expert University Scheduling Assistant.\n" +
                            "MISSION: Suggest exactly 3 VALID, CONFLICT-FREE schedule options.\n\n" +
                            "RULES:\n" +
                            "- You MUST strictly check the 'EXISTING BOOKINGS' list.\n" +
                            "- If a room is occupied at a given Day + Period, DO NOT suggest it.\n" +
                            "- Never invent rooms or times.\n" +
                            "- Each suggestion must include reasoning.\n\n" +
                            "OUTPUT FORMAT:\n" +
                            "1) Day | Periods | Room | Reason\n";
                    systemData = buildSchedulingContext(request.getPrompt());
                    break;
                case "FEEDBACK_GENERATOR":
                    aiPersona = "ROLE: You are an experienced university instructor. " +
                            "Generate a brief, professional, and constructive comment based on the student's score. " +
                            "If the score is low, suggest specific improvement strategies. If high, give specific praise.";
                    break;

                case "CURRICULUM_DESIGN":
                    aiPersona = "ROLE: You are an expert academic curriculum developer. " +
                            "Generate 5 specific, measurable Course Learning Objectives (CLOs) for the provided course title. "
                            +
                            "Use Bloom's Taxonomy verbs (e.g., Analyze, Design, Evaluate).";
                    break;

                case "GRADE_PROJECTION":
                    aiPersona = "ROLE: You are an academic advisor. " +
                            "Calculate exactly what score the student needs on remaining assessments to reach their target grade. "
                            +
                            "Use the provided current scores and assessment weights. Be mathematically precise.";

                    systemData = buildSystemContext();
                    break;
                case "STUDENT_ANALYSIS":
                case "GENERAL_QUERY":
                default:
                    // 1. Identify User
                    String email = SecurityContextHolder.getContext().getAuthentication().getName();
                    User user = userRepository.findByEmailAndIsDeletedFalse(email)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    String role = user.getRoles().iterator().next().getName();

                    // 2. Route to Specific Agent or General Context
                    systemData = buildSecureContext(user, role);

                    // 3. Build Persona
                    aiPersona = "You are SAPMS AI, an advanced academic assistant. " +
                               "Answer using ONLY the provided SYSTEM DATA. " +
                               "If data is missing, state that you do not have access to it.";
                    break;
            }

            String userContext = buildUserContext();

            String finalPrompt = String.format(
                    "%s\n\n=== SYSTEM DATABASE CONTEXT ===\n%s\n\n=== USER CONTEXT ===\n%s\n\n=== USER REQUEST ===\n%s",
                    aiPersona,
                    systemData,
                    userContext,
                    request.getPrompt());

            return callGeminiSdk(finalPrompt);

        } catch (Exception e) {
            log.error("AI Generation Error", e);
            return "I am currently unable to process your request. Please try again later.";
        }
    }

    private String buildSystemContext() {
        StringBuilder sb = new StringBuilder();

        // --- Stats ---
        sb.append("STATS:\n");
        sb.append("- Total Students: ").append(studentRepository.countByIsDeletedFalse()).append("\n");
        sb.append("- Total Instructors: ").append(instructorRepository.count()).append("\n");
        sb.append("- Total Courses: ").append(courseRepository.count()).append("\n");

        // --- Calendar ---
        sb.append("\nCALENDAR:\n");
        sb.append("Today: ").append(LocalDate.now()).append("\n");

        semesterRepository.findByIsDeletedFalse().stream()
                .filter(AcademicSemester::isCurrent)
                .findFirst()
                .ifPresentOrElse(
                        sem -> sb.append("Current Semester: ").append(sem.getName()).append("\n"),
                        () -> sb.append("Current Semester: None active\n"));

        // --- Grading ---
        sb.append("\nGRADING SCALES:\n");
        for (GradingScale scale : gradingScaleRepository.findAll()) {
            sb.append(String.format(
                    "- %s: %.0f–%.0f (GPA %.1f)\n",
                    scale.getLetterGrade(),
                    scale.getMinScore(),
                    scale.getMaxScore(),
                    scale.getGradePoint()));
        }

        // --- Attendance Rules ---
        sb.append("\nRULES:\n");
        sb.append("- Attendance < 75% = NG (No Grade)\n");

        // --- Departments ---
        sb.append("\nDEPARTMENTS:\n");
        for (Department dept : departmentRepository.findByIsDeletedFalse()) {
            sb.append(String.format(
                    "- %s: %d students, %d instructors\n",
                    dept.getName(),
                    studentRepository.countByDepartmentId(dept.getId()),
                    instructorRepository.countByDepartmentId(dept.getId())));
        }

        return sb.toString();
    }

    private String buildSchedulingContext(String userRequest) {
        StringBuilder sb = new StringBuilder();

        // --- Rooms ---
        sb.append("AVAILABLE ROOMS:\n");
        List<Room> rooms = roomRepository.findByIsDeletedFalse();
        for (Room r : rooms) {
            sb.append(String.format(
                    "- %s (%s, Capacity: %d)\n",
                    r.getName(),
                    r.getType(),
                    r.getCapacity() != null ? r.getCapacity() : 0));
        }

        // --- Conflict Map ---
        sb.append("\nEXISTING BOOKINGS (THESE ARE BUSY — DO NOT USE):\n");
        List<CourseSchedule> schedules = scheduleRepository.findAll();

        if (schedules.isEmpty()) {
            sb.append("No existing bookings.\n");
        } else {
            for (CourseSchedule cs : schedules) {
                try {
                    String courseCode = cs.getCourseOffering() != null &&
                            cs.getCourseOffering().getCourse() != null
                                    ? cs.getCourseOffering().getCourse().getCourseCode()
                                    : "Unknown Course";

                    sb.append(String.format(
                            "- BUSY: %s | Periods: %s | Room: %s | Course: %s\n",
                            cs.getDay(),
                            cs.getPeriods(),
                            cs.getRoom(),
                            courseCode));
                } catch (Exception ignored) {

                }
            }
        }

        // --- NEW: Add Scheduling Heuristics ---
        sb.append("\nSCHEDULING RULES & HEURISTICS:\n");
        sb.append("1. STANDARD DEPARTMENTS (CS, Business, Engineering, etc.):\n");
        sb.append("   - 3 Contact Hours: Usually 1 session (Periods 1-3) OR 2 sessions (1.5 hours not supported, so typically 3 straight).\n");
        sb.append("   - 4 Contact Hours: MUST SPLIT into 2 distinct sessions of 2 hours each (e.g., Mon 1-2 AND Wed 1-2).\n");
        sb.append("   - 5+ Contact Hours: Split into 2 or 3 sessions.\n");

        sb.append("2. ARCHITECTURE DEPARTMENT:\n");
        sb.append("   - Studio courses are taught in CONTINUOUS BLOCKS.\n");
        sb.append("   - 4 Contact Hours: Schedule as one block (e.g., Mon 1-4).\n");
        sb.append("   - 7 Contact Hours: Schedule as one block (e.g., Tue 1-7).\n");

        sb.append("\nINSTRUCTIONS:\n");
        sb.append("1. Check the 'Department' in the USER REQUEST.\n");
        sb.append("2. If Department is 'Architecture', use Rule 2. Otherwise, use Rule 1.\n");
        sb.append("3. For Split Sessions (Rule 1), ensure the instructor and room are available for BOTH slots.\n");
        sb.append("4. Suggest 3 options. Each option can contain multiple slots if split.\n");
        sb.append("5. Format: Option X: [Day Time Room] + [Day Time Room] (Reasoning)\n");

        return sb.toString();
    }

    private String buildUserContext() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            return userRepository.findByEmailAndIsDeletedFalse(email)
                    .map(u -> String.format(
                            "User: %s %s | Role: %s | Department: %s",
                            u.getFirstName(),
                            u.getLastName(),
                            u.getRoles().isEmpty() ? "None" : u.getRoles().iterator().next().getName(),
                            u.getDepartment() != null ? u.getDepartment().getName() : "None"))
                    .orElse("User: Anonymous");
        } catch (Exception e) {
            return "User context unavailable.";
        }
    }

    private String callGeminiSdk(String prompt) {
        try {
            Client client = Client.builder()
                    .apiKey(apiKey)
                    .build();

            GenerateContentResponse response = client.models.generateContent(
                    "gemini-2.5-flash",
                    prompt,
                    null);

            return response.text();

        } catch (Exception e) {
            log.error("Gemini API Error", e);
            return "AI service is temporarily unavailable.";
        }
    }

    // --- CONTEXT BUILDERS ---

    private String buildSecureContext(User user, String role) {
        StringBuilder sb = new StringBuilder();
        sb.append("Date: ").append(LocalDate.now()).append("\n");

        // Inject Unread Notifications for everyone
        long unread = notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
        sb.append("Notifications: You have ").append(unread).append(" unread alerts.\n\n");

        switch (role) {
            case "STUDENT":
                buildStudentContext(sb, user);
                break;
            case "INSTRUCTOR":
                buildInstructorContext(sb, user);
                break;
            case "DEPARTMENT_HEAD":
                buildDepartmentHeadContext(sb, user);
                break;
            case "SYSTEM_ADMIN":
            case "REGISTRAR":
                buildAdminContext(sb);
                break;
            default:
                sb.append("Role not configured for deep data access.");
        }
        return sb.toString();
    }

    private void buildStudentContext(StringBuilder sb, User user) {
        Student student = studentRepository.findByUserId(user.getId())
            .orElseThrow(() -> new RuntimeException("Student profile not found"));

        sb.append("--- ACADEMIC PROFILE ---\n");
        sb.append("Program: ").append(student.getProgram().getName()).append("\n");
        sb.append("Year Level: ").append(student.getYearLevel()).append("\n");
        sb.append("Section: ").append(student.getSection() != null ? student.getSection().getName() : "None").append("\n");

        sb.append("\n--- ACTIVE COURSES & GRADES ---\n");
        List<Enrollment> enrollments = enrollmentRepository.findByStudent_Id(student.getId().longValue());
        
        for (Enrollment e : enrollments) {
            String courseTitle = e.getCourseOffering().getCourse().getTitle();
            sb.append("Course: ").append(courseTitle).append("\n");
            
            // Grades
            List<Score> scores = scoreRepository.findByEnrollmentIdIn(List.of(e.getId()));
            for(Score s : scores) {
                sb.append(String.format("  - %s: %.2f / %.2f\n", 
                    s.getAssessment().getName(), s.getScoreValue(), s.getAssessment().getMaxScore()));
            }
            if (e.getFinalGrade() != null) {
                sb.append("  - FINAL GRADE: ").append(e.getFinalGrade()).append("\n");
            }
        }

        sb.append("\n--- GRADING SCALE REFERENCE ---\n");
        gradingScaleRepository.findAll().forEach(scale -> 
            sb.append(String.format("%s: %.0f-%.0f\n", scale.getLetterGrade(), scale.getMinScore(), scale.getMaxScore()))
        );
    }

    private void buildInstructorContext(StringBuilder sb, User user) {
        Instructor instructor = instructorRepository.findByUser_Id(user.getId())
            .orElseThrow(() -> new RuntimeException("Instructor not found"));

        sb.append("--- TEACHING LOAD ---\n");
        List<CourseOffering> offerings = courseOfferingRepository.findByInstructor_IdAndIsDeletedFalse(instructor.getId().intValue());
        
        for (CourseOffering co : offerings) {
            long count = enrollmentRepository.countByCourseOffering(co);
            sb.append(String.format("- %s (Section %s): %d students\n", 
                co.getCourse().getTitle(), co.getSection().getName(), count));
        }

        sb.append("\n--- PENDING REQUESTS ---\n");
        // Check for pending excuse requests
        Long pendingExcuses = excusedAbsenceRepository.findByInstructorId(instructor.getId().intValue(), org.springframework.data.domain.Pageable.unpaged())
                .stream().filter(r -> "Pending".equals(r.getStatus())).count();
        sb.append("Pending Absence Requests: ").append(pendingExcuses).append("\n");
    }

    private void buildDepartmentHeadContext(StringBuilder sb, User user) {
        if (user.getDepartment() == null) return;
        Long deptId = user.getDepartment().getId();

        sb.append("--- DEPARTMENT: ").append(user.getDepartment().getName()).append(" ---\n");
        sb.append("Total Students: ").append(studentRepository.countByDepartmentId(deptId)).append("\n");
        sb.append("Total Faculty: ").append(instructorRepository.countByDepartmentId(deptId)).append("\n");

        sb.append("\n--- APPROVAL QUEUE ---\n");
       
        long pendingGradeChanges = gradeChangeRepository.findByStatus("PENDING", org.springframework.data.domain.Pageable.unpaged())
                .stream()
                .filter(r -> r.getEnrollment().getCourseOffering().getCourse().getDepartment().getId().equals(deptId))
                .count();
        sb.append("Pending Grade Change Requests: ").append(pendingGradeChanges).append("\n");
    }

    private void buildAdminContext(StringBuilder sb) {
        sb.append("--- SYSTEM HEALTH ---\n");
        sb.append("Total Users: ").append(userRepository.count()).append("\n");
        sb.append("Total Courses: ").append(courseRepository.count()).append("\n");
        sb.append("Active Semester: ").append(semesterRepository.findByIsCurrentTrue().map(AcademicSemester::getName).orElse("None")).append("\n");
        
        sb.append("\n--- RECENT SECURITY LOGS ---\n");
        auditLogRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 5, org.springframework.data.domain.Sort.by("timestamp").descending()))
            .forEach(log -> sb.append(String.format("[%s] %s by %s\n", 
                log.getTimestamp(), log.getActionType(), log.getUser().getUsername())));
    }
}