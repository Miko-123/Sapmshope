package com.hopesapms.app.repository;

import com.hopesapms.app.model.CourseOffering;
import com.hopesapms.app.model.Enrollment;
import com.hopesapms.app.model.Student;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

       // For getting a student's own enrollments (fetches all related data)
       @Query("SELECT e FROM Enrollment e " +
                     "JOIN FETCH e.student s " +
                     "JOIN FETCH e.courseOffering co " +
                     "JOIN FETCH co.course c " +
                     "JOIN FETCH co.academicSemester " +
                     "JOIN FETCH co.instructor i " +
                     "JOIN FETCH i.user " +
                     "JOIN FETCH co.section " +
                     "WHERE s.id = :studentId")
       Page<Enrollment> findByStudentId(@Param("studentId") Integer studentId, Pageable pageable);

       // For the EnrollmentService (checks for duplicates)
       @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.courseOffering.id = :offeringId")
       Optional<Enrollment> findByStudentAndCourseOffering(
                     @Param("studentId") Integer studentId,
                     @Param("offeringId") Long offeringId);

       // For the EnrollmentService & PerformanceService (checks course history)
       @Query("SELECT e FROM Enrollment e " +
                     "JOIN FETCH e.courseOffering co " +
                     "JOIN FETCH co.academicSemester " +
                     "WHERE e.student.id = :studentId " +
                     "AND co.course.id = :courseId " +
                     "ORDER BY co.academicSemester.startDate DESC")
       List<Enrollment> findEnrollmentHistoryForCourse(
                     @Param("studentId") Integer studentId,
                     @Param("courseId") Integer courseId);

       // For the GradebookService (finds all students in one offering)
       @Query("SELECT e FROM Enrollment e " +
                     "JOIN FETCH e.student s " +
                     "JOIN FETCH s.user " +
                     "WHERE e.courseOffering.id = :offeringId")
       List<Enrollment> findByCourseOfferingId(@Param("offeringId") Long offeringId);

       boolean existsByStudentAndCourseOffering(Student student, CourseOffering courseOffering);

       long countByCourseOffering(CourseOffering courseOffering);

       long countByCourseOfferingIdAndFinalGradeIsNull(Long offeringId);

       List<Enrollment> findByStudent_IdAndStatus(Long studentId, String status);

       List<Enrollment> findByStudent_Id(Long studentId);

       long countByCourseOffering_AcademicSemester_IdAndStatus(Long semesterId, String status);

       long countByCourseOffering_AcademicSemester_IdAndFinalGradeIsNull(Long semesterId);
}