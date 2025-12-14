package com.hopesapms.app.controller;

import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.Assessment;
import com.hopesapms.app.model.CourseOffering;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.Score;
import com.hopesapms.app.repository.AssessmentRepository;
import com.hopesapms.app.repository.CourseOfferingRepository;
import com.hopesapms.app.repository.EnrollmentRepository;
import com.hopesapms.app.repository.ScoreRepository;
import com.hopesapms.app.service.InstructorService;
import com.hopesapms.app.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/instructor")
@RequiredArgsConstructor
@Tag(name = "Instructor Portal", description = "APIs for instructors to view courses and students")
public class InstructorController {

    private final InstructorService instructorService;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;
    private final ReportService reportService;

    @GetMapping("/courses")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD')") // Dept heads are often instructors too
    @Operation(summary = "Get courses assigned to the logged-in instructor")
    public ResponseEntity<List<CourseOfferingResponseDTO>> getMyCourses(Authentication authentication) {
        return ResponseEntity.ok(instructorService.getMyCourses(authentication.getName()));
    }

    @GetMapping("/courses/{offeringId}/students")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD')")
    @Operation(summary = "Get the list of students enrolled in a specific course offering")
    public ResponseEntity<List<StudentResponse>> getClassList(
            @PathVariable Long offeringId,
            Authentication authentication) {
        return ResponseEntity.ok(instructorService.getClassList(offeringId, authentication.getName()));
    }

    @GetMapping("/dashboard/stats")
    @PreAuthorize("hasAnyAuthority('INSTRUCTOR', 'DEPARTMENT_HEAD')")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(instructorService.getInstructorDashboardStats(authentication.getName()));
    }

    @PostMapping("/courses/{courseId}/grades/submit")
    @PreAuthorize("hasAuthority('INSTRUCTOR')")
    @Operation(summary = "Calculate and save final grades for all students")
    public ResponseEntity<String> submitFinalGrades(
            @PathVariable Long courseId,
            Authentication authentication) {

        instructorService.submitFinalGrades(courseId, authentication.getName());
        return ResponseEntity.ok("Grades submitted successfully. Transcripts updated.");
    }

    @GetMapping("/courses/{courseId}/grades/download")
    @PreAuthorize("hasAuthority('INSTRUCTOR')")
    public ResponseEntity<byte[]> downloadGradeReport(@PathVariable Long courseId) throws IOException {

        CourseOffering course = courseOfferingRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found"));

        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(courseId);

        List<Assessment> assessments = assessmentRepository
                .findByCourseOfferingIdAndIsDeletedFalse(course.getId());

        List<Score> allScores = scoreRepository.findByEnrollmentIdIn(
                enrollments.stream().map(Enrollment::getId).collect(Collectors.toList()));

        byte[] pdfBytes = reportService.generateCourseGradeReport(course, enrollments, assessments, allScores);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=Grade_Report_" + course.getCourse().getCourseCode() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}