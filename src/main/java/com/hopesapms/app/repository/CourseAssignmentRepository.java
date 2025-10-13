package com.hopesapms.app.repository;

import com.hopesapms.app.model.CourseAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseAssignmentRepository extends JpaRepository<CourseAssignment, Integer> {
    @Query("SELECT ca FROM CourseAssignment ca WHERE ca.isDeleted = false")
    Page<CourseAssignment> findAllActive(Pageable pageable);

    @Query("SELECT ca FROM CourseAssignment ca JOIN FETCH ca.instructor WHERE ca.instructor.id = :instructorId AND ca.isDeleted = false")
    Page<CourseAssignment> findByInstructorId(Integer instructorId, Pageable pageable);

    @Query("SELECT ca FROM CourseAssignment ca JOIN FETCH ca.course WHERE ca.course.id = :courseId AND ca.isDeleted = false")
    List<CourseAssignment> findByCourseId(Integer courseId);
}