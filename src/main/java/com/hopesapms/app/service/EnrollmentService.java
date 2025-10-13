package com.hopesapms.app.service;

import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.repository.EnrollmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class EnrollmentService {
    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Transactional
    public Enrollment enrollStudent(Enrollment enrollment) {
        Optional<Enrollment> existing = Optional.ofNullable(enrollmentRepository.findByStudentIdAndCourseId(enrollment.getStudent().getId(), enrollment.getCourse().getId()));
        if (existing.isPresent() && !existing.get().isDeleted()) {
            throw new IllegalArgumentException("Student already enrolled in this course");
        }
        return enrollmentRepository.save(enrollment);
    }

    @Transactional(readOnly = true)
    public Page<Object []> getGpaByStudent(Pageable pageable) {
        return enrollmentRepository.findAverageGpaByStudent(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Enrollment> findByStudentId(Integer studentId, Pageable pageable) {
        return enrollmentRepository.findByStudentId(studentId, pageable);
    }
}
