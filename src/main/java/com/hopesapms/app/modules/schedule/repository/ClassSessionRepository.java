package com.hopesapms.app.modules.schedule.repository;

import com.hopesapms.app.modules.schedule.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Integer> {

    Optional<ClassSession> findByCourseOfferingIdAndSessionDate(Long offeringId, LocalDate sessionDate);

    List<ClassSession> findByCourseOfferingIdOrderBySessionDateDesc(Long offeringId);

    Optional<ClassSession> findByCourseOffering_IdAndSessionDate(Long courseOfferingId, LocalDate sessionDate);

@Query("SELECT cs FROM ClassSession cs " +
       "JOIN FETCH cs.courseOffering co " +
       "JOIN FETCH co.instructor i " + 
       "JOIN FETCH i.user u " +          
       "JOIN FETCH u.department d " +    
       "JOIN co.academicSemester s " + 
       "WHERE cs.isDeleted = false " +
       "AND co.isDeleted = false " +
       "AND s.isCurrent = true") // Updated to use isCurrent
List<ClassSession> findAllActiveSessionsWithDetails();
}