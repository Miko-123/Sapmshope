package com.hopesapms.app.service;

import com.hopesapms.app.dto.SemesterRolloverRequest;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.AcademicSemester;
import com.hopesapms.app.model.CourseOffering;
import com.hopesapms.app.repository.AcademicSemesterRepository;
import com.hopesapms.app.repository.CourseOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterRolloverService {

    private final CourseOfferingRepository offeringRepository;
    private final AcademicSemesterRepository semesterRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public int rolloverSemester(SemesterRolloverRequest request) {
        
        AcademicSemester source = semesterRepository.findById(request.getSourceSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Source semester not found"));
        
        AcademicSemester target = semesterRepository.findById(request.getTargetSemesterId())
                .orElseThrow(() -> new ResourceNotFoundException("Target semester not found"));

        if (target.isArchived()) {
            throw new IllegalStateException("Cannot rollover into an ARCHIVED semester.");
        }

        List<CourseOffering> sourceOfferings = offeringRepository.findByAcademicSemester_IdAndIsDeletedFalse(source.getId());

        int count = 0;

        for (CourseOffering original : sourceOfferings) {
            boolean exists = offeringRepository.existsByAcademicSemester_IdAndCourse_IdAndSection_IdAndIsDeletedFalse(
                    target.getId(), 
                    original.getCourse().getId(), 
                    original.getSection().getId()
            );

            if (!exists) {
                CourseOffering clone = CourseOffering.builder()
                        .academicSemester(target) 
                        .course(original.getCourse())
                        .section(original.getSection())
                        .yearLevels(original.getYearLevels())
                        .contactHours(original.getContactHours())
                        .status("PLANNED")
                        .instructor(request.isCopyInstructors() ? original.getInstructor() : null) 
                        .build();

                offeringRepository.save(clone);
                count++;
            }
        }

        auditLogService.log("SEMESTER_ROLLOVER", "AcademicSemester", target.getId(), 
                "Source: " + source.getName(), "Created " + count + " offerings.");

        return count;
    }
}