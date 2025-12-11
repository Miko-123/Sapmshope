package com.hopesapms.app.repository;

import com.hopesapms.app.model.ClassSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

@Repository
public interface ClassSessionRepository extends JpaRepository<ClassSession, Integer> {

    Optional<ClassSession> findByCourseOfferingIdAndSessionDate(Long offeringId, LocalDate sessionDate);

    List<ClassSession> findByCourseOfferingIdOrderBySessionDateDesc(Long offeringId);

    Optional<ClassSession> findByCourseOffering_IdAndSessionDate(Long courseOfferingId, LocalDate sessionDate);
}