package com.hopesapms.app.service;

import com.hopesapms.app.model.Assessment;
import com.hopesapms.app.repository.AssessmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AssessmentService {
    @Autowired
    private AssessmentRepository assessmentRepository;

    @Transactional
    public Assessment createAssessment(Assessment assessment){
        if (assessment.getDueDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Due date cannot be in the past");
        }
        return assessmentRepository.save(assessment);
    }

    @Transactional(readOnly = true)
    public Page<Assessment> findOverdueByCourseId(Integer courseId, Pageable pageable) {
        return assessmentRepository.findOverdueByCourseId(courseId, pageable);
    }

    @Transactional(readOnly = true)
    public double getAverageMaxScoreByCourseId(Integer courseId) {
        return assessmentRepository.findAverageMaxScoreByCourseId(courseId);
    }
}
