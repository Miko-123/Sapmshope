package com.hopesapms.app.repository;

import com.hopesapms.app.model.Assessment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends JpaRepository<Assessment, Integer> {
    
    @Query("SELECT a FROM Assessment a WHERE a.isDeleted = false")
    Page<Assessment> findAllActive(Pageable pageable);

    Optional<Assessment> findByIdAndIsDeletedFalse(Integer id);

    List<Assessment> findByCourseOfferingIdAndIsDeletedFalse(Long courseOfferingId);

    @Query("SELECT a FROM Assessment a JOIN FETCH a.courseOffering co WHERE co.id = :offeringId AND a.isDeleted = false")
    Page<Assessment> findByCourseOfferingIdAndIsDeletedFalse(@Param("offeringId") Long offeringId, Pageable pageable);

    @Query("SELECT a FROM Assessment a JOIN FETCH a.courseOffering co WHERE co.id = :offeringId AND a.dueDate < CURRENT_TIMESTAMP AND a.isDeleted = false")
    Page<Assessment> findOverdueByCourseOfferingId(@Param("offeringId") Long offeringId, Pageable pageable);

    @Query("SELECT AVG(a.maxScore) FROM Assessment a WHERE a.courseOffering.id = :offeringId AND a.isDeleted = false")
    Double findAverageMaxScoreByCourseOfferingId(@Param("offeringId") Long offeringId);
}