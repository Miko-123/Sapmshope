package com.hopesapms.app.repository;

import com.hopesapms.app.model.StudentAttendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Integer> {
    @Query("SELECT sa FROM StudentAttendance sa WHERE sa.isDeleted = false")
    Page<StudentAttendance> findAllActive(Pageable pageable);

    @Query("SELECT sa FROM StudentAttendance sa JOIN FETCH sa.enrollment e JOIN FETCH e.student s " +
           "WHERE e.id = :enrollmentId AND sa.isDeleted = false")
    Page<StudentAttendance> findByEnrollmentId(Integer enrollmentId, Pageable pageable);

    @Query("SELECT sa FROM StudentAttendance sa JOIN FETCH sa.classSession cs JOIN FETCH cs.course c " +
           "WHERE cs.id = :classSessionId AND sa.isDeleted = false")
    Page<StudentAttendance> findByClassSessionId(Integer classSessionId, Pageable pageable);

    @Query("SELECT sa.enrollment.student.id, COUNT(sa) * 100.0 / (SELECT COUNT(cs) FROM ClassSession cs WHERE cs.course.id = c.id AND cs.isDeleted = false) AS attendancePercentage " +
           "FROM StudentAttendance sa JOIN sa.enrollment e JOIN e.courseOffering co JOIN co.course c JOIN sa.classSession cs " +
           "WHERE sa.status = 'Present' AND sa.isDeleted = false AND e.isDeleted = false AND c.isDeleted = false " +
           "GROUP BY sa.enrollment.student.id, c.id")
    Page<Object[]> findAttendancePercentageByStudent(Pageable pageable);
}