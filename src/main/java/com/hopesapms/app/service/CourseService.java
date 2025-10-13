package com.hopesapms.app.service;

import com.hopesapms.app.model.Course;
import com.hopesapms.app.repository.CourseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CourseService {
    @Autowired
    private CourseRepository courseRepository;

    @Transactional
    public Course saveCourse(Course course){
        if (courseRepository.findByCourseCodeAndIsDeletedFalse(course.getCourseCode()).isPresent()) {
            throw new IllegalArgumentException("Course code already exists");
        }
        return courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public Page<Course> findAllActive(Pageable pageable){
        return courseRepository.findAllActive(pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Course> findByIdWithAssessment(Integer id) {
        return courseRepository.findByIdWithAssessments(id);
    }

    @Transactional(readOnly = true)
    public double getAverageCredits() {
        return courseRepository.findAverageCredits();
    }
}
