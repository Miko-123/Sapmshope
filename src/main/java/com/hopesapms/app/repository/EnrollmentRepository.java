package com.hopesapms.app.repository;

import com.hopesapms.app.dto.DepartmentSemesterGpaRawDto;
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
import java.util.Map;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

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

       @Query("SELECT e.finalGrade as label, COUNT(e) as value " +
                     "FROM Enrollment e " +
                     "WHERE e.courseOffering.course.department.id = :deptId " +
                     "AND e.finalGrade IS NOT NULL " +
                     "GROUP BY e.finalGrade")
       List<Map<String, Object>> findGradeDistributionByDepartment(@Param("deptId") Long deptId);

       @Query("SELECT e FROM Enrollment e WHERE e.student.id = :studentId AND e.courseOffering.id = :offeringId")
       Optional<Enrollment> findByStudentAndCourseOffering(
                     @Param("studentId") Integer studentId,
                     @Param("offeringId") Long offeringId);

       @Query("SELECT e FROM Enrollment e " +
                     "JOIN FETCH e.courseOffering co " +
                     "JOIN FETCH co.academicSemester " +
                     "WHERE e.student.id = :studentId " +
                     "AND co.course.id = :courseId " +
                     "ORDER BY co.academicSemester.startDate DESC")
       List<Enrollment> findEnrollmentHistoryForCourse(
                     @Param("studentId") Integer studentId,
                     @Param("courseId") Integer courseId);

       @Query("SELECT e FROM Enrollment e " +
                     "JOIN FETCH e.student s " +
                     "JOIN FETCH s.user " +
                     "WHERE e.courseOffering.id = :offeringId")
       List<Enrollment> findByCourseOfferingId(@Param("offeringId") Long offeringId);

       @Query("SELECT e.courseOffering.academicSemester.name as label, COUNT(e) as value " +
                     "FROM Enrollment e " +
                     "GROUP BY e.courseOffering.academicSemester.name " +
                     "ORDER BY e.courseOffering.academicSemester.startDate ASC")
       List<Map<String, Object>> findEnrollmentTrends();

       boolean existsByStudentAndCourseOffering(Student student, CourseOffering courseOffering);

       long countByCourseOffering(CourseOffering courseOffering);

       long countByCourseOfferingIdAndFinalGradeIsNull(Long offeringId);

       List<Enrollment> findByStudent_IdAndStatus(Long studentId, String status);

       List<Enrollment> findByStudent_Id(Long studentId);

       long countByCourseOffering_AcademicSemester_IdAndStatus(Long semesterId, String status);

       long countByCourseOffering_AcademicSemester_IdAndFinalGradeIsNull(Long semesterId);

@Query("""
    SELECT new com.hopesapms.app.dto.DepartmentSemesterGpaRawDto(
        sem.name,
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
            END * co.course.credits
        ) / SUM(co.course.credits),
        COUNT(DISTINCT s.id)
    )
    FROM Enrollment e
    JOIN e.student s
    JOIN s.department d
    JOIN e.courseOffering co
    JOIN co.academicSemester sem
    WHERE e.finalGrade IS NOT NULL
      AND s.status = 'ACTIVE'
    GROUP BY sem.year, sem.name, d.name, sem.startDate
    ORDER BY sem.year ASC, sem.startDate ASC, d.name ASC 
""")
List<DepartmentSemesterGpaRawDto> findDepartmentPerformanceStats();
}