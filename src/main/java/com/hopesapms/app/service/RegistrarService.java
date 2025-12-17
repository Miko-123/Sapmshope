package com.hopesapms.app.service;

import com.hopesapms.app.dto.RegistrarDashboardDTO;
import com.hopesapms.app.exception.ResourceNotFoundException;
import com.hopesapms.app.model.AcademicSemester;
import com.hopesapms.app.repository.AcademicSemesterRepository;
import com.hopesapms.app.repository.CourseOfferingRepository;
import com.hopesapms.app.repository.EnrollmentRepository;
import com.hopesapms.app.repository.StudentRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class RegistrarService {

    private final AcademicSemesterRepository semesterRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;

    @Transactional(readOnly = true)
    public RegistrarDashboardDTO getDashboardStats() {

        AcademicSemester current = semesterRepository.findByIsCurrentTrue()
                .orElseThrow(() -> new ResourceNotFoundException("No active semester found."));

        long activeOfferings = offeringRepository.countByAcademicSemesterIdAndIsDeletedFalse(current.getId());

        
        long totalStudents = studentRepository.countByIsDeletedFalse();


        long pendingGrades = enrollmentRepository.countByCourseOffering_AcademicSemester_IdAndFinalGradeIsNull(
                current.getId());

    
        double progress = calculateProgress(current.getStartDate(), current.getEndDate());

        return RegistrarDashboardDTO.builder()
                .currentSemesterId(current.getId())
                .currentSemesterName(current.getName())
                .semesterStatus(current.getStatus())
                .totalActiveOfferings(activeOfferings)
                .totalEnrolledStudents(totalStudents)
                .studentsWithPendingGrades(pendingGrades)
                .semesterProgressPercentage(progress)
                .build();
    }

    private double calculateProgress(LocalDate start, LocalDate end) {
        if (start == null || end == null) return 0.0;
        
        LocalDate now = LocalDate.now();
        if (now.isBefore(start)) return 0.0;
        if (now.isAfter(end)) return 100.0;

        long totalDays = ChronoUnit.DAYS.between(start, end);
        long daysPassed = ChronoUnit.DAYS.between(start, now);

        if (totalDays <= 0) return 100.0;

        return Math.min(100.0, (double) daysPassed / totalDays * 100.0);
    }
}