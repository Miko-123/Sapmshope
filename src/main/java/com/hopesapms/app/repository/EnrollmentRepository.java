package com.hopesapms.app.repository;

import com.hopesapms.app.model.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {
    @Query("SELECT e FROM Enrollment e WHERE e.isDeleted = false")
    Page<Enrollment> findAllActive(Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course WHERE e.student.id = :studentId AND e.isDeleted = false")
    Page<Enrollment> findByStudentId(Integer studentId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.course WHERE e.course.id = :courseId AND e.isDeleted = false")
    Page<Enrollment> findByCourseId(Integer courseId, Pageable pageable);

    @Query("SELECT e FROM Enrollment e JOIN FETCH e.student JOIN FETCH e.course WHERE e.student.id = :studentId AND e.course.id = :courseId AND e.isDeleted = false")
    Enrollment findByStudentIdAndCourseId(Integer studentId, Integer courseId);

    @Query("SELECT e.student.id, AVG(CAST(s.scoreValue AS float)) FROM Enrollment e LEFT JOIN Score s ON s.enrollment.id = e.id " +
           "WHERE e.isDeleted = false GROUP BY e.student.id")
    Page<Object[]> findAverageGpaByStudent(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Enrollment e WHERE e.course.id = :courseId AND e.isDeleted = false")
    Long countEnrollmentsByCourseId(Integer courseId);
}