package com.hopesapms.app.service;

import com.hopesapms.app.dto.BulkEnrollmentRowDTO;
import com.hopesapms.app.dto.ImportResultDTO;
import com.hopesapms.app.dto.EnrollmentRequestDTO;
import com.hopesapms.app.dto.EnrollmentResponseDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.*;
import com.hopesapms.app.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final ExcelImportService excelImportService;
    private final AuditLogService auditLogService;
    
    
    private final CourseOfferingRepository courseOfferingRepository; 

    @Transactional
    public EnrollmentResponseDTO enrollStudent(EnrollmentRequestDTO dto) {

        Student student = studentRepository.findById(dto.getStudentId())
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + dto.getStudentId()));

        CourseOffering courseOffering = courseOfferingRepository.findById(dto.getCourseOfferingId())
                .orElseThrow(() -> new ResourceNotFoundException("Course Offering not found with id: " + dto.getCourseOfferingId()));

        
        enrollmentRepository
                .findByStudentAndCourseOffering(student.getId(), courseOffering.getId())
                .ifPresent(e -> {
                    throw new IllegalArgumentException("Student " + student.getId() + " is already enrolled in offering " + courseOffering.getId());
                });

        boolean isAdd = false;
        Course courseToHistoryCheck = courseOffering.getCourse(); 
        List<Enrollment> history = enrollmentRepository
                .findEnrollmentHistoryForCourse(student.getId(), courseToHistoryCheck.getId());

        if (!history.isEmpty()) {
            Enrollment mostRecent = history.get(0);
            if ("COMPLETED".equals(mostRecent.getStatus()) && !"F".equals(mostRecent.getFinalGrade())) {
                throw new IllegalArgumentException("Student has already passed this course: " + courseToHistoryCheck.getCourseCode());
            }
            if ("F".equals(mostRecent.getFinalGrade())) {
                isAdd = true;
            }
        }

        Enrollment newEnrollment = Enrollment.builder()
                .student(student)
                .courseOffering(courseOffering)
                .enrollmentDate(LocalDate.now())
                .status("ENROLLED")
                .isAddStudent(isAdd)
                .build();

        Enrollment savedEnrollment = enrollmentRepository.save(newEnrollment);
        auditLogService.log("ENROLL_STUDENT", "Enrollment", savedEnrollment.getId().longValue(), null,
                "Registrar enrolled Student " + student.getId() + " in Offering " + courseOffering.getId());

        return mapToResponseDTO(savedEnrollment);
    }

    @Transactional
    public ImportResultDTO bulkEnrollStudents(MultipartFile file) {
        List<BulkEnrollmentRowDTO> rows;
        try {
            
            rows = excelImportService.parseEnrollments(file);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse Excel file: " + e.getMessage());
        }

        int successfulEnrollments = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < rows.size(); i++) {
            BulkEnrollmentRowDTO row = rows.get(i);
           
            String rowIdentifier = "Row " + (i + 2); 

            try {
                Student student = studentRepository.findByStudentId(row.getStudentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + row.getStudentId()));

                CourseOffering offering = courseOfferingRepository.findById(row.getCourseOfferingId())
                        .orElseThrow(() -> new ResourceNotFoundException("CourseOffering not found with ID: " + row.getCourseOfferingId()));

                if (enrollmentRepository.existsByStudentAndCourseOffering(student, offering)) {
                    
                    errors.add(rowIdentifier + ": Student " + row.getStudentId() + " is already enrolled.");
                    continue; 
                }

                Enrollment newEnrollment = new Enrollment();
                newEnrollment.setStudent(student);
                newEnrollment.setCourseOffering(offering);
                newEnrollment.setStatus("ENROLLED");
                
                enrollmentRepository.save(newEnrollment);
                successfulEnrollments++;

            } catch (Exception e) {
                errors.add(rowIdentifier + ": " + e.getMessage());
            }
        }
        
        return new ImportResultDTO(successfulEnrollments, 0, errors);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponseDTO> getMyEnrollments(Authentication authentication, Pageable pageable) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Logged-in user not found."));

        Student student = studentRepository.findByUserId(user.getId())
                .orElseThrow(() -> new AccessDeniedException("User is not a student."));

        Page<Enrollment> enrollmentPage = enrollmentRepository.findByStudentId(student.getId(), pageable);

        return enrollmentPage.map(this::mapToResponseDTO);
    }

    private EnrollmentResponseDTO mapToResponseDTO(Enrollment e) {
        EnrollmentResponseDTO dto = new EnrollmentResponseDTO();
        dto.setEnrollmentId(e.getId());
        dto.setEnrollmentDate(e.getEnrollmentDate());
        dto.setStatus(e.getStatus());
        dto.setFinalGrade(e.getFinalGrade());
        dto.setAddStudent(e.isAddStudent());

        
        CourseOffering offering = e.getCourseOffering();
        dto.setCourseOfferingId(offering.getId());
        dto.setSemesterName(offering.getAcademicSemester().getName());
        dto.setInstructorName(offering.getInstructor().getUser().getFirstName() + " " + offering.getInstructor().getUser().getLastName());
        dto.setSectionName(offering.getSection().getName());
        dto.setCourseCode(offering.getCourse().getCourseCode());
        dto.setCourseTitle(offering.getCourse().getTitle());

        return dto;
    }
}