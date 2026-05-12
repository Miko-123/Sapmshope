package com.hopesapms.app.modules.score.service;

import com.hopesapms.app.modules.score.dto.BulkScoreRequestDTO;
import com.hopesapms.app.modules.enrollment.dto.BulkUploadResponse;
import com.hopesapms.app.modules.department.dto.DepartmentGpaDto;
import com.hopesapms.app.modules.department.dto.DepartmentPerformanceDto;
import com.hopesapms.app.modules.department.dto.DepartmentSemesterGpaRawDto;
import com.hopesapms.app.modules.score.dto.ScoreRequestDTO;
import com.hopesapms.app.modules.score.dto.ScoreResponseDTO;
import com.hopesapms.app.modules.score.dto.StudentScoreDTO;
import com.hopesapms.app.common.exception.ResourceNotFoundException;
import com.hopesapms.app.modules.assessment.model.Assessment;
import com.hopesapms.app.modules.department.model.Department;
import com.hopesapms.app.modules.enrollment.model.Enrollment;
import com.hopesapms.app.modules.score.model.Score;
import com.hopesapms.app.modules.student.model.Student;
import com.hopesapms.app.modules.user.model.User;
import com.hopesapms.app.modules.assessment.repository.AssessmentRepository;
import com.hopesapms.app.modules.enrollment.repository.EnrollmentRepository;
import com.hopesapms.app.modules.score.repository.ScoreRepository;
import com.hopesapms.app.modules.student.repository.StudentRepository;
import com.hopesapms.app.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import com.hopesapms.app.common.event.AppEvents;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.hopesapms.app.modules.academicsemester.service.AcademicSemesterService;
import com.hopesapms.app.modules.auditlog.service.AuditLogService;

@Service
@RequiredArgsConstructor
public class ScoreService {

        private final ScoreRepository scoreRepository;
        private final EnrollmentRepository enrollmentRepository;
        private final AssessmentRepository assessmentRepository;
        private final UserRepository userRepository;
        private final StudentRepository studentRepository;
        private final AuditLogService auditLogService;
        private final AcademicSemesterService semesterService;
        private final ApplicationEventPublisher eventPublisher;

        @Transactional
        public ScoreResponseDTO enterScore(ScoreRequestDTO dto, Authentication authentication) {

                User instructor = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

                Enrollment enrollment = enrollmentRepository.findById(dto.getEnrollmentId())
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Enrollment not found with id: "
                                                                + dto.getEnrollmentId()));

                Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(dto.getAssessmentId())
                                .orElseThrow(
                                                () -> new ResourceNotFoundException("Assessment not found with id: "
                                                                + dto.getAssessmentId()));
                Long semesterId = assessment.getCourseOffering().getAcademicSemester().getId();
                semesterService.validateSemesterEditable(semesterId);

                checkUserAuthorityForCourse(instructor,
                                assessment.getCourseOffering().getCourse().getDepartment().getId(),
                                "enter score for");

                if (dto.getScoreValue().compareTo(assessment.getMaxScore()) > 0) {
                        throw new IllegalArgumentException("Score (" + dto.getScoreValue()
                                        + ") cannot be greater than the assessment's max score ("
                                        + assessment.getMaxScore() + ")");
                }

                Optional<Score> existingScoreOpt = scoreRepository
                                .findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(dto.getEnrollmentId(),
                                                dto.getAssessmentId());

                Score score;
                String oldData = null;
                String action = "ENTER_SCORE";

                if (existingScoreOpt.isPresent()) {
                        score = existingScoreOpt.get();
                        oldData = score.toString();
                        action = "UPDATE_SCORE";

                        score.setOriginalScoreValue(score.getScoreValue()); // Archive old score
                        score.setScoreValue(dto.getScoreValue());
                        score.setOverriden(true);

                } else {
                        score = Score.builder()
                                        .enrollment(enrollment)
                                        .assessment(assessment)
                                        .scoreValue(dto.getScoreValue())
                                        .isOverriden(false)
                                        .build();
                }

                score.setRecordedBy(instructor);
                score.setRecordedDate(LocalDateTime.now());

                Score savedScore = scoreRepository.save(score);

                User studentUser = savedScore.getEnrollment().getStudent().getUser();
                eventPublisher.publishEvent(new AppEvents.GradePostedEvent(
                                this, studentUser,
                                savedScore.getEnrollment().getCourseOffering().getCourse().getTitle(),
                                savedScore.getAssessment().getName(), savedScore.getScoreValue().doubleValue()));

                auditLogService.log(action, "Score", savedScore.getId().longValue(), oldData, savedScore.toString());

                return mapToResponseDTO(savedScore);
        }

        @Transactional
        public BulkUploadResponse bulkEnterScores(BulkScoreRequestDTO bulkDto, Authentication authentication) {

                User instructor = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

                Assessment assessment = assessmentRepository.findByIdAndIsDeletedFalse(bulkDto.getAssessmentId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Assessment not found with id: " + bulkDto.getAssessmentId()));

                checkUserAuthorityForCourse(instructor,
                                assessment.getCourseOffering().getCourse().getDepartment().getId(),
                                "bulk enter scores for");

                List<String> errors = new ArrayList<>();
                int successCount = 0;
                int failedCount = 0;

                for (BulkScoreRequestDTO.StudentScoreEntry entry : bulkDto.getScores()) {
                        try {
                                ScoreRequestDTO singleDto = new ScoreRequestDTO();
                                singleDto.setEnrollmentId(entry.getEnrollmentId());
                                singleDto.setAssessmentId(bulkDto.getAssessmentId());
                                singleDto.setScoreValue(entry.getScoreValue());

                                enterScore(singleDto, authentication);
                                successCount++;

                        } catch (Exception e) {
                                failedCount++;
                                errors.add("EnrollmentID " + entry.getEnrollmentId() + ": FAILED | Error: "
                                                + e.getMessage());
                        }
                }

                BulkUploadResponse response = new BulkUploadResponse();
                response.setSuccessCount(successCount);
                response.setFailedCount(failedCount);
                response.setErrors(errors);

                auditLogService.log("BULK_ENTER_SCORES", "Score", assessment.getId().longValue(), null,
                                "Success: " + successCount + ", Failed: " + failedCount);
                return response;
        }

        private void checkUserAuthorityForCourse(User user, Long departmentId, String action) {
                boolean isSystemAdmin = user.getRoles().stream()
                                .anyMatch(role -> "SYSTEM_ADMIN".equals(role.getName()));
                if (isSystemAdmin)
                        return;

                boolean isInstructor = user.getRoles().stream()
                                .anyMatch(role -> "INSTRUCTOR".equals(role.getName()));
                boolean isDeptHead = user.getRoles().stream()
                                .anyMatch(role -> "DEPARTMENT_HEAD".equals(role.getName()));

                if ((isInstructor || isDeptHead) && user.getDepartment() != null
                                && user.getDepartment().getId().equals(departmentId)) {
                        return;
                }
                throw new AccessDeniedException("You do not have permission to " + action + " this course.");
        }

        @Transactional(readOnly = true)
        public List<StudentScoreDTO> getMyScoresForCourse(Integer courseId, Authentication authentication) {

                User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

                Student student = studentRepository.findByUserId(user.getId())
                                .orElseThrow(() -> new AccessDeniedException("This user is not a student."));

                Page<Score> scores = scoreRepository.findByStudentAndCourse(student.getId(), courseId,
                                Pageable.unpaged());

                return scores.stream()
                                .map(this::mapToStudentScoreDTO)
                                .collect(Collectors.toList());
        }

        @Transactional(readOnly = true)
        public List<DepartmentGpaDto> getAverageGpaPerDepartment() {
                // 1. Fetch raw data: All enrollments with a grade in the current semester
                List<Enrollment> enrollments = enrollmentRepository.findAll().stream()
                                .filter(e -> e.getFinalGrade() != null
                                                && !e.isDeleted()
                                                && e.getCourseOffering().getAcademicSemester().isCurrent())
                                .collect(Collectors.toList());

                // 2. Group by Department Name
                Map<String, List<Enrollment>> deptGroup = enrollments.stream()
                                .collect(Collectors.groupingBy(
                                                e -> e.getCourseOffering().getCourse().getDepartment().getName()));

                List<DepartmentGpaDto> report = new ArrayList<>();

                // 3. Calculate GPA manually in Java
                for (Map.Entry<String, List<Enrollment>> entry : deptGroup.entrySet()) {
                        String deptName = entry.getKey();
                        List<Enrollment> deptEnrollments = entry.getValue();

                        // Get Department ID safely
                        Long deptId = deptEnrollments.isEmpty() ? 0L
                                        : deptEnrollments.get(0).getCourseOffering().getCourse().getDepartment()
                                                        .getId();

                        double totalWeightedPoints = 0.0;
                        double totalCredits = 0.0;

                        for (Enrollment e : deptEnrollments) {
                                Double credits = e.getCourseOffering().getCourse().getCredits();
                                double gradePoint = convertGradeToPoint(e.getFinalGrade()); // <--- Conversion happens
                                                                                            // here

                                if (credits != null) {
                                        totalWeightedPoints += (gradePoint * credits);
                                        totalCredits += credits;
                                }
                        }

                        double avgGpa = (totalCredits > 0) ? (totalWeightedPoints / totalCredits) : 0.0;

                        // Round to 2 decimal places
                        avgGpa = Math.round(avgGpa * 100.0) / 100.0;

                        report.add(new DepartmentGpaDto(deptId, deptName, avgGpa));
                }

                return report;
        }

        public List<DepartmentSemesterGpaRawDto> getDepartmentSemesterGpa() {
                return enrollmentRepository.findDepartmentPerformanceStats();
        }

        public List<DepartmentPerformanceDto> getDepartmentPerformance() {

                List<DepartmentSemesterGpaRawDto> raw = enrollmentRepository.findDepartmentPerformanceStats();

                // Group by department
                Map<String, List<DepartmentSemesterGpaRawDto>> byDept = raw.stream()
                                .collect(Collectors.groupingBy(DepartmentSemesterGpaRawDto::getDepartment));

                List<DepartmentPerformanceDto> result = new ArrayList<>();

                for (var entry : byDept.entrySet()) {
                        String department = entry.getKey();
                        List<DepartmentSemesterGpaRawDto> records = entry.getValue();

                        // Sort by semester order (already ordered, but safe)
                        records.sort(Comparator.comparing(DepartmentSemesterGpaRawDto::getSemester));

                        for (int i = 0; i < records.size(); i++) {
                                DepartmentSemesterGpaRawDto current = records.get(i);
                                Double previousGpa = (i == 0) ? null : records.get(i - 1).getGpa();
                                Double change = (previousGpa == null)
                                                ? null
                                                : round(current.getGpa() - previousGpa);

                                result.add(new DepartmentPerformanceDto(
                                                department,
                                                current.getSemester(),
                                                round(current.getGpa()),
                                                current.getEnrolledStudents(),
                                                change,
                                                determineStatus(change)));
                        }
                }

                return result;
        }

        private Double round(Double value) {
                return value == null ? null : Math.round(value * 100.0) / 100.0;
        }

        private String determineStatus(Double change) {
                if (change == null)
                        return "N/A";
                if (change >= 0.30)
                        return "Excellent";
                if (change >= 0.10)
                        return "Good";
                if (change >= -0.3)
                        return "Needs Attention";
                return "Bad";
        }

        private StudentScoreDTO mapToStudentScoreDTO(Score s) {
                StudentScoreDTO dto = new StudentScoreDTO();

                Assessment assessment = s.getAssessment();
                dto.setAssessmentName(assessment.getName());
                dto.setAssessmentType(assessment.getType());
                dto.setMaxScore(assessment.getMaxScore());

                dto.setScoreId(s.getId());
                dto.setScoreValue(s.getScoreValue());
                dto.setRecordedDate(s.getRecordedDate());
                if (s.getRecordedBy() != null) {
                        dto.setRecordedBy(s.getRecordedBy().getUsername());
                }

                return dto;
        }

        private ScoreResponseDTO mapToResponseDTO(Score s) {
                ScoreResponseDTO dto = new ScoreResponseDTO();
                dto.setId(s.getId());
                dto.setEnrollmentId(s.getEnrollment().getId());
                dto.setAssessmentId(s.getAssessment().getId());
                dto.setScoreValue(s.getScoreValue());
                if (s.getRecordedBy() != null) {
                        dto.setRecordedByUsername(s.getRecordedBy().getUsername());
                }
                dto.setRecordedDate(s.getRecordedDate());
                dto.setOverriden(s.isOverriden());
                dto.setOriginalScoreValue(s.getOriginalScoreValue());
                return dto;
        }

        private double convertGradeToPoint(String grade) {
                if (grade == null)
                        return 0.0;
                switch (grade.toUpperCase()) {
                        case "A":
                                return 4.0;
                        case "A-":
                                return 3.75;
                        case "B+":
                                return 3.5;
                        case "B":
                                return 3.0;
                        case "B-":
                                return 2.75;
                        case "C+":
                                return 2.5;
                        case "C":
                                return 2.0;
                        case "C-":
                                return 1.75;
                        case "D":
                                return 1.0;
                        case "F":
                                return 0.0;
                        default:
                                return 0.0;
                }
        }
}
