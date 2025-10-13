package com.hopesapms.app.repository;

import com.hopesapms.app.model.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {
    @Query("SELECT s FROM Score s WHERE s.isDeleted = false")
    Page<Score> findAllActive(Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.enrollment WHERE s.enrollment.id = :enrollmentId AND s.isDeleted = false")
    Page<Score> findByEnrollmentId(Integer enrollmentId, Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.assessment WHERE s.assessment.id = :assessmentId AND s.isDeleted = false")
    Page<Score> findByAssessmentId(Integer assessmentId, Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.enrollment e WHERE e.student.id = :studentId AND e.course.id = :courseId AND s.isDeleted = false")
    Page<Score> findByStudentAndCourse(Integer studentId, Integer courseId, Pageable pageable);

    @Query("SELECT AVG(s.scoreValue) FROM Score s JOIN s.enrollment e WHERE e.student.id = :studentId AND s.isDeleted = false")
    Double findAverageScoreByStudentId(Integer studentId);
}