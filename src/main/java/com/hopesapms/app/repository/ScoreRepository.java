package com.hopesapms.app.repository;

import com.hopesapms.app.dto.ScoreResponseDTO;
import com.hopesapms.app.model.Assessment;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.Instructor;
import com.hopesapms.app.model.Score;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Integer> {

    Optional<Score> findByEnrollment_IdAndAssessment_IdAndIsDeletedFalse(Integer enrollmentId, Integer assessmentId);

    @Query("""
    SELECT new com.hopesapms.app.dto.ScoreResponseDTO(
        s.id,
        st.id,
        CONCAT(u.firstName,' ',u.middleName,' ',u.lastName),
        a.id,
        e.id,
        a.name,
        s.scoreValue,
        e.finalGrade,
        c.id,
        c.title,
        sec.id,
        sec.name
    )
    FROM Instructor i
    JOIN i.courseOfferings co
    JOIN co.section sec
    JOIN co.course c
    JOIN c.assessments a
    JOIN a.scores s
    JOIN s.enrollment e
    JOIN e.student st
    JOIN st.user u
    WHERE i.user.username = :instructorUsername
    AND sec.id = :sectionId
    AND c.isDeleted = false
    AND c.title = :courseTitle
""")
    Page<ScoreResponseDTO> getStudentScores(
            @Param("sectionId") Integer sectionId,
            @Param("instructorUsername") String instructorUsername,
            @Param("courseTitle") String courseTitle,
            Pageable pageable);

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

    Optional<Score> findByEnrollmentAndAssessment(Enrollment enrollment, Assessment assessment);
}