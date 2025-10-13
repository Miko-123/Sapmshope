package com.hopesapms.app.repository;

import com.hopesapms.app.model.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
    @Query("SELECT a FROM Assessment a WHERE a.isDeleted = false")
    Page<Assessment> findAllActive(Pageable pageable);

    @Query("SELECT a FROM Assessment a JOIN FETCH a.course WHERE a.course.id = :courseId AND a.isDeleted = false")
    Page<Assessment> findByCourseId(Integer courseId, Pageable pageable);

    @Query("SELECT a FROM Assessment a JOIN FETCH a.course WHERE a.course.id = :courseId AND a.dueDate < CURRENT_TIMESTAMP AND a.isDeleted = false")
    Page<Assessment> findOverdueByCourseId(Integer courseId, Pageable pageable);

    @Query("SELECT AVG(a.maxScore) FROM Assessment a WHERE a.course.id = :courseId AND a.isDeleted = false")
    Double findAverageMaxScoreByCourseId(Integer courseId);
}