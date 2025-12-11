package com.hopesapms.app.repository;

import com.hopesapms.app.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {
    List<Attendance> findByClassSessionId(Integer sessionId);

    @Query("SELECT a FROM Attendance a WHERE a.student.id = :studentId AND a.classSession.courseOffering.id = :courseOfferingId")
    List<Attendance> findByStudentAndCourse(@Param("studentId") Integer studentId,
            @Param("courseOfferingId") Long courseOfferingId);
}