package com.hopesapms.app.repository;

import com.hopesapms.app.model.ClassSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Integer> {
    @Query("SELECT cs FROM ClassSession cs WHERE cs.isDeleted = false")
    Page<ClassSession> findAllActive(Pageable pageable);

    @Query("SELECT cs FROM ClassSession cs JOIN FETCH cs.course WHERE cs.course.id = :courseId AND cs.isDeleted = false")
    Page<ClassSession> findByCourseId(Integer courseId, Pageable pageable);

    @Query("SELECT cs FROM ClassSession cs JOIN FETCH cs.scheduledInstructor WHERE cs.scheduledInstructor.id = :instructorId AND cs.isDeleted = false")
    Page<ClassSession> findByScheduledInstructorId(Integer instructorId, Pageable pageable);

    @Query("SELECT COUNT(cs) FROM ClassSession cs WHERE cs.course.id = :courseId AND cs.isDeleted = false")
    Long countSessionsByCourseId(Integer courseId);
}