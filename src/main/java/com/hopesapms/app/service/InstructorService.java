package com.hopesapms.app.service;

import com.hopesapms.app.dto.CourseOfferingResponseDTO;
import com.hopesapms.app.dto.StudentResponse;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final GradingService gradingService;
    private final AssessmentRepository assessmentRepository;
    private final ScoreRepository scoreRepository;

    @Transactional(readOnly = true)
    public List<CourseOfferingResponseDTO> getMyCourses(String email) {
        Instructor instructor = getInstructorByEmail(email);

        List<CourseOffering> offerings = courseOfferingRepository.findByInstructorIdAndStatus(instructor.getId(),
                "ACTIVE");
        offerings.addAll(courseOfferingRepository.findByInstructorIdAndStatus(instructor.getId(), "PLANNED"));

        return offerings.stream()
                .map(this::mapToOfferingDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudentResponse> getClassList(Long offeringId, String email) {
        Instructor instructor = getInstructorByEmail(email);

        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Course offering not found"));

        if (!offering.getInstructor().getId().equals(instructor.getId())) {
            throw new AccessDeniedException("You are not authorized to view this class list.");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(offeringId);

        return enrollments.stream()
                .map(e -> mapToStudentDTO(e.getStudent()))
                .collect(Collectors.toList());
    }

    private Instructor getInstructorByEmail(String email) {
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        return instructorRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Instructor profile not found for user: " + email));
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getInstructorDashboardStats(String email) {
        Instructor instructor = getInstructorByEmail(email);
        List<CourseOffering> activeCourses = courseOfferingRepository.findByInstructorIdAndStatus(instructor.getId(),
                "ACTIVE");

        int totalCourses = activeCourses.size();

        long totalStudents = 0;
        for (CourseOffering offering : activeCourses) {
            totalStudents += enrollmentRepository.countByCourseOffering(offering);
        }

        long pendingGrades = activeCourses.stream()
                .mapToLong(c -> enrollmentRepository.countByCourseOfferingIdAndFinalGradeIsNull(c.getId()))
                .sum();

        int totalHours = activeCourses.stream().mapToInt(CourseOffering::getContactHours).sum();

        return Map.of(
                "totalCourses", totalCourses,
                "totalStudents", totalStudents,
                "pendingGrades", pendingGrades,
                "totalContactHours", totalHours);
    }

    @Transactional
    public void submitFinalGrades(Long courseOfferingId, String instructorEmail) {

        List<Enrollment> enrollments = enrollmentRepository.findByCourseOfferingId(courseOfferingId);
        List<Assessment> assessments = assessmentRepository.findByCourseOfferingIdAndIsDeletedFalse(courseOfferingId);

        for (Enrollment e : enrollments) {
            double totalScore = 0.0;
            boolean hasScores = false;

            for (Assessment a : assessments) {
                Optional<Score> scoreOpt = scoreRepository.findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(
                        e.getId().intValue(), a.getId());
                if (scoreOpt.isPresent()) {
                    totalScore += scoreOpt.get().getScoreValue().doubleValue();
                    hasScores = true;
                }
            }

            GradingScale gradeScale = gradingService.calculateGrade(totalScore);

            if (gradeScale != null) {
                e.setFinalGrade(gradeScale.getLetterGrade());
                enrollmentRepository.save(e);
            }
        }
    }

    private CourseOfferingResponseDTO mapToOfferingDTO(CourseOffering c) {
        CourseOfferingResponseDTO dto = new CourseOfferingResponseDTO();
        dto.setId(c.getId());
        dto.setCourseId(c.getCourse().getId().intValue());
        dto.setCourseTitle(c.getCourse().getTitle());
        dto.setCourseCode(c.getCourse().getCourseCode());
        dto.setSectionName(c.getSection().getName());
        dto.setSemesterName(c.getAcademicSemester().getName());
        dto.setStatus(c.getStatus());
        dto.setSectionYearLevel(c.getSection().getYearLevel());
        ;
        dto.setContactHours(c.getContactHours());
        return dto;
    }

    private StudentResponse mapToStudentDTO(Student s) {
        StudentResponse dto = new StudentResponse();
        dto.setId(s.getId());
        dto.setStudentId(s.getStudentId());
        dto.setFirstName(s.getUser().getFirstName());
        dto.setLastName(s.getUser().getLastName());
        dto.setEmail(s.getUser().getEmail());
        dto.setStatus(s.getStatus());
        return dto;
    }
}