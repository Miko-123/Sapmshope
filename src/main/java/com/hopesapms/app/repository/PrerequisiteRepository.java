package com.hopesapms.app.repository;

import com.hopesapms.app.model.Prerequisite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrerequisiteRepository extends JpaRepository<Prerequisite, Integer> {

    Page<Prerequisite> findByIsDeletedFalse(Pageable pageable);

    
    @Query("SELECT p FROM Prerequisite p " +
           "JOIN FETCH p.course c " +
           "JOIN FETCH p.prerequisiteCourse pc " +
           "WHERE c.id = :courseId AND p.isDeleted = false")
    List<Prerequisite> findByCourseIdWithDetails(@Param("courseId") Integer courseId);

    Optional<Prerequisite> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByCourse_IdAndPrerequisiteCourse_IdAndIsDeletedFalse(Integer courseId, Integer prerequisiteCourseId);
}