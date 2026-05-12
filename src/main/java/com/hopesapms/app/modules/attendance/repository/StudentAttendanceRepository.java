package com.hopesapms.app.modules.attendance.repository;

import com.hopesapms.app.modules.attendance.model.Attendance;
import com.hopesapms.app.modules.attendance.model.StudentAttendance;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentAttendanceRepository extends JpaRepository<StudentAttendance, Integer> {
    @Query("SELECT sa FROM Attendance sa " +
           "JOIN FETCH sa.classSession cs " +
           "JOIN FETCH cs.courseOffering co " + // <--- FIXED HERE
           "WHERE cs.id = :classSessionId")     // Removed isDeleted check if not in entity
    Page<Attendance> findByClassSessionId(Integer classSessionId, Pageable pageable);
    
    // Non-paginated version if needed
    List<Attendance> findByClassSessionId(Integer sessionId);
}