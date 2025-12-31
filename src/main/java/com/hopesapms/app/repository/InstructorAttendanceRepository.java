package com.hopesapms.app.repository;

import com.hopesapms.app.model.ClassSession;
import com.hopesapms.app.model.InstructorAttendance;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface InstructorAttendanceRepository extends JpaRepository<InstructorAttendance, Integer> {
    @Query("SELECT ia FROM InstructorAttendance ia WHERE ia.isDeleted = false")
    Page<InstructorAttendance> findAllActive(Pageable pageable);

    @Query("SELECT ia FROM InstructorAttendance ia JOIN FETCH ia.instructor WHERE ia.instructor.id = :instructorId AND ia.isDeleted = false")
    Page<InstructorAttendance> findByInstructorId(Integer instructorId, Pageable pageable);

    @Query("SELECT ia FROM InstructorAttendance ia JOIN FETCH ia.classSession WHERE ia.classSession.id = :classSessionId AND ia.isDeleted = false")
    InstructorAttendance findByClassSessionId(Integer classSessionId);


}