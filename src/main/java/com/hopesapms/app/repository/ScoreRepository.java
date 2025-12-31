package com.hopesapms.app.repository;

import com.hopesapms.app.dto.DepartmentGpaDto;
import com.hopesapms.app.model.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    Optional<Score> findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(Integer enrollmentId, Integer assessmentId);

    @Query("SELECT s FROM Score s WHERE s.isDeleted = false")
    Page<Score> findAllActive(Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.enrollment WHERE s.enrollment.id = :enrollmentId AND s.isDeleted = false")
    Page<Score> findByEnrollmentId(Integer enrollmentId, Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.assessment WHERE s.assessment.id = :assessmentId AND s.isDeleted = false")
    Page<Score> findByAssessmentId(Integer assessmentId, Pageable pageable);

    @Query("SELECT s FROM Score s JOIN FETCH s.enrollment e " +
            "WHERE e.student.id = :studentId " +
            "AND e.courseOffering.course.id = :courseId " +
            "AND s.isDeleted = false")
    Page<Score> findByStudentAndCourse(Integer studentId, Integer courseId, Pageable pageable);

    @Query("SELECT AVG(s.scoreValue) FROM Score s JOIN s.enrollment e WHERE e.student.id = :studentId AND s.isDeleted = false")
    Double findAverageScoreByStudentId(Integer studentId);

    Optional<Score> findByEnrollmentIdAndAssessmentId(Long enrollmentId, Integer assessmentId);

    @Query("SELECT s FROM Score s WHERE s.enrollment.id IN :enrollmentIds")
    List<Score> findByEnrollmentIdIn(List<Integer> enrollmentIds);

    @Query("""
                SELECT new com.hopesapms.app.dto.DepartmentGpaDto(
                    d.id,
                    d.name,
                    SUM(
                        CASE e.finalGrade
                            WHEN 'A+' THEN 4.0
                            WHEN 'A'  THEN 4.0
                            WHEN 'A-' THEN 3.7
                            WHEN 'B+' THEN 3.3
                            WHEN 'B'  THEN 3.0
                            WHEN 'B-' THEN 2.7
                            WHEN 'C+' THEN 2.3
                            WHEN 'C'  THEN 2.0
                            WHEN 'C-' THEN 1.7
                            WHEN 'D'  THEN 1.0
                            ELSE 0.0
                        END * c.credits
                    ) / SUM(c.credits)
                )
                FROM Enrollment e
                JOIN e.courseOffering co
                JOIN co.course c
                JOIN e.student s
                JOIN s.department d
                WHERE e.finalGrade IS NOT NULL
                GROUP BY d.id, d.name
            """)
    List<DepartmentGpaDto> findWeightedAverageGpaPerDepartment();

}